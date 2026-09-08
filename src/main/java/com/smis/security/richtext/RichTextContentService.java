package com.smis.security.richtext;

import com.smis.entity.Installment;
import com.smis.entity.InstallmentNew;
import com.smis.entity.Work;
import org.springframework.stereotype.Service;

@Service
public class RichTextContentService {
    private final RichTextHtmlSanitizer sanitizer;

    public RichTextContentService(RichTextHtmlSanitizer sanitizer) {
        this.sanitizer = sanitizer;
    }

    public void prepare(Installment installment) {
        installment.setCopyTo(sanitizer.sanitizeForStorage(installment.getCopyTo()));
    }

    public void prepare(InstallmentNew installment) {
        installment.setCopyTo(sanitizer.sanitizeForStorage(installment.getCopyTo()));
    }

    public void prepare(Work work) {
        // An uninitialized detached collection contains no caller edits. The converter
        // still protects every child Hibernate actually writes during merge/flush.
        if (work.getInstallments() != null && org.hibernate.Hibernate.isInitialized(work.getInstallments())) {
            work.getInstallments().forEach(this::prepare);
        }
    }
}
