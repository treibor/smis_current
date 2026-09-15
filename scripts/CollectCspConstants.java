import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Offline inventory of compile-time strings in the resolved UI dependencies (never request data). */
class CollectCspConstants {
    public static void main(String[] args) throws Exception {
        Map<String, String> constants = new TreeMap<>();
        for (String file : Files.readString(Path.of("target/security-classpath.txt")).trim().split(";")) {
            String name = Path.of(file).getFileName().toString();
            if (!name.matches("(flow-|vaadin-|so-|vcf-|ckeditor-).*\\.jar")) continue;
            try (ZipFile jar = new ZipFile(file)) {
                var entries = jar.entries();
                while (entries.hasMoreElements()) {
                    var entry = entries.nextElement();
                    if (!entry.getName().endsWith(".class")) continue;
                    try (DataInputStream in = new DataInputStream(jar.getInputStream(entry))) {
                        if (in.readInt() != 0xcafebabe) continue;
                        in.readInt();
                        int size = in.readUnsignedShort();
                        String[] utf = new String[size];
                        List<Integer> strings = new ArrayList<>();
                        for (int i = 1; i < size; i++) {
                            switch (in.readUnsignedByte()) {
                                case 1 -> utf[i] = in.readUTF();
                                case 8 -> strings.add(in.readUnsignedShort());
                                case 3, 4, 9, 10, 11, 12, 17, 18 -> in.skipNBytes(4);
                                case 5, 6 -> { in.skipNBytes(8); i++; }
                                case 7, 16, 19, 20 -> in.skipNBytes(2);
                                case 15 -> in.skipNBytes(3);
                                default -> throw new IOException("Unsupported constant pool");
                            }
                        }
                        // Annotation values (including @EventData) reference UTF8 directly.
                        for (int index : strings) constants.putIfAbsent(utf[index], name + ":" + entry.getName());
                        for (String value : utf) if (value != null && value.matches("(?s).*\\b(event|element)\\..*"))
                            constants.putIfAbsent(value, name + ":" + entry.getName());
                    }
                }
            }
        }
        // Generate the application's Enter/Escape shortcuts with the resolved Flow implementation.
        var urls = new ArrayList<java.net.URL>();
        for (String file : Files.readString(Path.of("target/security-classpath.txt")).trim().split(";"))
            urls.add(Path.of(file).toUri().toURL());
        try (var loader = new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new))) {
            var key = loader.loadClass("com.vaadin.flow.component.Key");
            var shortcuts = loader.loadClass("com.vaadin.flow.component.ShortcutRegistration");
            var keyFilter = shortcuts.getDeclaredMethod("generateEventKeyFilter", key);
            var modifiers = shortcuts.getDeclaredMethod("generateEventModifierFilter", Collection.class);
            var template = shortcuts.getDeclaredField("ELEMENT_LOCATOR_JS");
            keyFilter.setAccessible(true); modifiers.setAccessible(true); template.setAccessible(true);
            for (String name : List.of("ENTER", "ESCAPE")) {
                String filter = keyFilter.invoke(null, key.getField(name).get(null)) + " && "
                        + modifiers.invoke(null, List.of());
                for (String prevent : List.of("", " && (event.preventDefault() || true)"))
                    for (String stop : List.of("", " && (event.stopPropagation() || true)"))
                        constants.put(filter + prevent + stop, "Flow ShortcutRegistration: " + name);
                for (String locator : List.of("this.$.overlay", "this._overlayElement"))
                    for (String prevent : List.of("", "event.preventDefault();"))
                        constants.put(String.format((String)template.get(null), locator, filter, prevent),
                                "Flow ShortcutRegistration: " + name + " / " + locator);
            }
        }
        StringBuilder json = new StringBuilder("[");
        for (var entry : constants.entrySet()) {
            if (json.length() > 1) json.append(',');
            json.append("{\"code\":").append(quote(entry.getKey())).append(",\"origin\":").append(quote(entry.getValue())).append('}');
        }
        Files.writeString(Path.of("target/csp-constants.json"), json.append(']'));
    }
    static String quote(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            if (c == '"' || c == '\\') out.append('\\').append(c);
            else if (c < 32) out.append(String.format("\\u%04x", (int)c));
            else out.append(c);
        }
        return out.append('"').toString();
    }
}
