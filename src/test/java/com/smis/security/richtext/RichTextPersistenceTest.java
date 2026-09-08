package com.smis.security.richtext;

import static org.assertj.core.api.Assertions.*;
import com.smis.dbservice.Dbservice;
import com.smis.dbservice.NewService;
import com.smis.entity.Installment;
import com.smis.entity.InstallmentNew;
import com.smis.entity.Work;
import com.smis.repository.InstallmentRepository;
import com.smis.repository.InstallmentNewRepository;
import com.smis.repository.WorkRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.util.ReflectionTestUtils;

class RichTextPersistenceTest {
    private static LocalContainerEntityManagerFactoryBean factory;
    private EntityManager em;
    private Dbservice service;
    private InstallmentRepository repository;
    private InstallmentNewRepository newRepository;

    @BeforeAll
    static void database() {
        factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(new DriverManagerDataSource("jdbc:h2:mem:richtext;DB_CLOSE_DELAY=-1;NON_KEYWORDS=YEAR;MODE=PostgreSQL", "sa", ""));
        factory.setPackagesToScan("com.smis.entity");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        // Domain validation is unrelated to HTML; use the actual entities/converters/repositories with minimal fixtures.
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop",
                "jakarta.persistence.validation.mode", "none", "hibernate.validator.apply_to_ddl", "false",
                "hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"));
        factory.afterPropertiesSet();
    }

    @AfterAll static void closeFactory() { if (factory != null) factory.destroy(); }

    @BeforeEach
    void begin() {
        em = factory.getObject().createEntityManager();
        em.getTransaction().begin();
        JpaRepositoryFactory repositories = new JpaRepositoryFactory(em);
        repository = repositories.getRepository(InstallmentRepository.class);
        newRepository = repositories.getRepository(InstallmentNewRepository.class);
        service = new Dbservice(null, null, repositories.getRepository(WorkRepository.class), null, null, null,
                null, null, repository, null, null, null, null, null, null);
        ReflectionTestUtils.setField(service, "richTextContent", new RichTextContentService(RichTextHtmlSanitizer.INSTANCE));
    }

    @AfterEach void rollback() { em.getTransaction().rollback(); em.close(); }

    @Test
    void uiBypassCreateAndEditPersistOnlySanitizedHtml() {
        Installment installment = new Installment();
        installment.setCopyTo("<p onclick=alert(1)>Safe<script>alert(1)</script></p>");
        service.saveInstallment(installment);
        em.flush();
        assertThat(raw("installment", installment.getInstallmentId())).isEqualTo("<p>Safe</p>");
        installment.setCopyTo("<a href='javascript:alert(1)'>Click</a>");
        service.saveInstallment(installment);
        em.flush();
        assertThat(raw("installment", installment.getInstallmentId())).isEqualTo("<a>Click</a>");
    }

    @Test
    void processServiceAndDirectRepositorySaveAllAreProtected() {
        NewService process = new NewService();
        ReflectionTestUtils.setField(process, "irepo", newRepository);
        ReflectionTestUtils.setField(process, "richTextContent", new RichTextContentService(RichTextHtmlSanitizer.INSTANCE));
        InstallmentNew newer = new InstallmentNew();
        newer.setCopyTo("<div onclick=alert(1)>Safe</div>");
        process.saveInstallment(newer);
        Installment direct = new Installment();
        direct.setCopyTo("<p>Safe<img src=x onerror=alert(1)></p>");
        repository.saveAll(List.of(direct));
        em.flush();
        assertThat(raw("installment_new", newer.getInstallmentId())).isEqualTo("<div>Safe</div>");
        assertThat(raw("installment", direct.getInstallmentId())).isEqualTo("<p>Safe</p>");
    }

    @Test
    void cascadesAndDirtyCheckingCannotBypassPolicy() {
        Work work = new Work();
        Installment installment = new Installment();
        installment.setWork(work);
        installment.setCopyTo("<p onclick=alert(1)>Safe</p>");
        work.setInstallments(List.of(installment));
        service.saveWork(work);
        em.flush();
        assertThat(raw("installment", installment.getInstallmentId())).isEqualTo("<p>Safe</p>");
        installment.setCopyTo("<p onmouseover=alert(2)>Updated</p>");
        em.flush();
        assertThat(raw("installment", installment.getInstallmentId())).isEqualTo("<p>Updated</p>");
    }

    @Test
    void oversizedInputFailsBeforeRepositoryPersistence() {
        Installment installment = new Installment();
        installment.setCopyTo("x".repeat(2001));
        assertThatThrownBy(() -> service.saveInstallment(installment)).isInstanceOf(IllegalArgumentException.class);
        assertThat(installment.getInstallmentId()).isZero();
    }

    private String raw(String table, long id) {
        return (String) em.createNativeQuery("select copy_to from " + table + " where installment_id = :id")
                .setParameter("id", id).getSingleResult();
    }
}
