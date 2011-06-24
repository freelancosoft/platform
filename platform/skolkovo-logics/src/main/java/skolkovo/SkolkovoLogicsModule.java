package skolkovo;

//import com.smartgwt.client.docs.Files;
import jasperapi.ReportGenerator;
import net.sf.jasperreports.engine.JRAbstractExporter;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.action.ClientAction;
import platform.interop.action.MessageClientAction;
import platform.interop.form.RemoteFormInterface;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.server.classes.*;
import platform.server.data.Union;
import platform.server.data.expr.query.OrderType;
import platform.server.data.query.Query;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.*;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.ContainerView;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.form.window.ToolBarNavigatorWindow;
import platform.server.form.window.TreeNavigatorWindow;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.DataObject;
import platform.server.logics.LogicsModule;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LP;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.Property;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.mail.EmailActionProperty;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.Modifier;
import skolkovo.actions.ImportProjectsActionProperty;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.List;

import static platform.base.BaseUtils.nvl;

/**
 * User: DAle
 * Date: 24.05.11
 * Time: 18:21
 */


public class SkolkovoLogicsModule extends LogicsModule {

    public SkolkovoLogicsModule(BaseLogicsModule<SkolkovoBusinessLogics> baseLM) {
        super("SkolkovLogicsModule");
        setBaseLogicsModule(baseLM);
    }

    private LP inExpertVoteDateFromDateTo;
    private LP quantityInExpertDateFromDateTo;
    private LP emailClaimerAcceptedHeaderVote;
    private LP emailClaimerNameVote;
    private LP emailClaimerRejectedHeaderVote;
    private LP emailClaimerAcceptedHeaderProject;
    private LP emailAcceptedProjectEA;
    private LP emailAcceptedProject;
    public LP sidProject;
    public LP sidToProject;
    public LP nameNativeToClaimer;
    public LP nameNativeToCluster;
    public LP nameNativeCluster;
    private LP nameForeignCluster;
    public LP dateProject;
    public LP nativeNumberToPatent;

    AbstractCustomClass multiLanguageNamed;

    public ConcreteCustomClass project;
    ConcreteCustomClass expert;
    public ConcreteCustomClass cluster;
    public ConcreteCustomClass claimer;
    ConcreteCustomClass documentTemplate;
    ConcreteCustomClass documentAbstract;
    ConcreteCustomClass documentTemplateDetail;
    ConcreteCustomClass document;

    public ConcreteCustomClass nonRussianSpecialist;
    public ConcreteCustomClass academic;
    public ConcreteCustomClass patent;

    ConcreteCustomClass vote;

    public StaticCustomClass projectType;
    StaticCustomClass language;
    StaticCustomClass documentType;
    public StaticCustomClass ownerType;

    StaticCustomClass voteResult;
    StaticCustomClass projectStatus;

    AbstractGroup projectInformationGroup;
    AbstractGroup additionalInformationGroup;
    AbstractGroup innovationGroup;
    AbstractGroup executiveSummaryGroup;
    AbstractGroup sourcesFundingGroup;
    AbstractGroup equipmentGroup;
    AbstractGroup projectDocumentsGroup;
    AbstractGroup projectStatusGroup;

    AbstractGroup voteResultGroup;
    AbstractGroup voteResultCheckGroup;
    AbstractGroup voteResultCommentGroup;

    AbstractGroup contactGroup;
    AbstractGroup documentGroup;
    AbstractGroup legalDataGroup;
    AbstractGroup claimerInformationGroup;


    AbstractGroup expertResultGroup;

    @Override
    public void initClasses() {
        initBaseClassAliases();

        multiLanguageNamed = addAbstractClass("multiLanguageNamed", "Многоязычный объект", baseClass);

        projectType = addStaticClass("projectType", "Тип проекта",
                                     new String[]{"comparable3", "surpasses3", "russianbenchmark3", "certainadvantages3", "significantlyoutperforms3", "nobenchmarks3"},
                                     new String[]{"сопоставим с существующими российскими аналогами или уступает им", "превосходит российские аналоги, но уступает лучшим зарубежным аналогам",
                                                  "Является российским аналогом востребованного зарубежного продукта/технологии", "Обладает отдельными преимуществами над лучшими мировыми аналогами, но в целом сопоставим с ними",
                                                  "Существенно превосходит все существующие мировые аналоги", "Не имеет аналогов, удовлетворяет ранее не удовлетворенную потребность и создает новый рынок"});

        ownerType = addStaticClass("ownerType", "Тип правообладателя",
                                               new String[]{"employee", "participant", "thirdparty"},
                                               new String[]{"Работником организации", "Участником организации", "Третьим лицом"});

        patent = addConcreteClass("patent", "Патент", baseClass);
        academic = addConcreteClass("academic", "Научные кадры", baseClass);
        nonRussianSpecialist = addConcreteClass("nonRussianSpecialist", "Иностранный специалист", baseClass);

        project = addConcreteClass("project", "Проект", multiLanguageNamed, baseLM.transaction);
        expert = addConcreteClass("expert", "Эксперт", baseLM.customUser);
        cluster = addConcreteClass("cluster", "Кластер", multiLanguageNamed);

        claimer = addConcreteClass("claimer", "Заявитель", multiLanguageNamed, baseLM.emailObject);
        claimer.dialogReadOnly = false;

        documentTemplate = addConcreteClass("documentTemplate", "Шаблон документов", baseClass.named);

        documentAbstract = addConcreteClass("documentAbstract", "Документ (абстр.)", baseClass);

        documentTemplateDetail = addConcreteClass("documentTemplateDetail", "Документ (прототип)", documentAbstract);

        document = addConcreteClass("document", "Документ", documentAbstract);

        vote = addConcreteClass("vote", "Заседание", baseClass, baseLM.transaction);

        language = addStaticClass("language", "Язык",
                new String[]{"russian", "english"},
                new String[]{"Русский", "Английский"});

        voteResult = addStaticClass("voteResult", "Результат заседания",
                                    new String[]{"refused", "connected", "voted"},
                                    new String[]{"Отказался", "Аффилирован", "Проголосовал"});

        projectStatus = addStaticClass("projectStatus", "Статус проекта",
                                       new String[]{"unknown", "needDocuments", "needExtraVote", "inProgress", "succeeded", "accepted", "rejected"},
                                       new String[]{"Неизвестный статус", "Не соответствуют документы", "Требуется заседание", "Идет заседание", "Достаточно голосов", "Оценен положительно", "Оценен отрицательно"});

        documentType = addStaticClass("documentType", "Тип документа",
                new String[]{"application", "resume", "techdesc", "forres"},
                new String[]{"Анкета", "Резюме", "Техническое описание", "Резюме иностранного специалиста "});
    }

    @Override
    public void initTables() {
        baseLM.tableFactory.include("multiLanguageNamed", multiLanguageNamed);
        baseLM.tableFactory.include("project", project);
        baseLM.tableFactory.include("expert", expert);
        baseLM.tableFactory.include("cluster", cluster);
        baseLM.tableFactory.include("claimer", claimer);
        baseLM.tableFactory.include("vote", vote);
        baseLM.tableFactory.include("patent", patent);
        baseLM.tableFactory.include("academic", academic);
        baseLM.tableFactory.include("nonRussianSpecialist", nonRussianSpecialist);
        baseLM.tableFactory.include("documentAbstract", documentAbstract);
        baseLM.tableFactory.include("document", document);
        baseLM.tableFactory.include("expertVote", expert, vote);
    }

    @Override
    public void initGroups() {
        initBaseGroupAliases();
        contactGroup = addAbstractGroup("contactGroup", "Контакты организации", publicGroup);

        documentGroup = addAbstractGroup("documentGroup", "Юридические документы", publicGroup);

        legalDataGroup = addAbstractGroup("legalDataGroup", "Юридические данные", publicGroup);

        claimerInformationGroup = addAbstractGroup("claimerInformationGroup", "Информация о заявителе", publicGroup);


        projectInformationGroup = addAbstractGroup("projectInformationGroup", "Информация по проекту", baseGroup);

        additionalInformationGroup = addAbstractGroup("additionalInformationGroup", "Доп. информация по проекту", publicGroup);

        innovationGroup = addAbstractGroup("innovationGroup", "Инновация", baseGroup);

        executiveSummaryGroup = addAbstractGroup("executiveSummaryGroup", "Резюме проекта", baseGroup);

        sourcesFundingGroup = addAbstractGroup("sourcesFundingGroup", "Источники финансирования", baseGroup);

        equipmentGroup = addAbstractGroup("equipmentGroup", "Оборудование", baseGroup);

        projectDocumentsGroup = addAbstractGroup("projectDocumentsGroup", "Документы", baseGroup);

        projectStatusGroup = addAbstractGroup("projectStatusGroup", "Текущий статус проекта", baseGroup);

        voteResultGroup = addAbstractGroup("voteResultGroup", "Результаты голосования", publicGroup);

        expertResultGroup = addAbstractGroup("expertResultGroup", "Статистика по экспертам", publicGroup);

        voteResultCheckGroup = addAbstractGroup("voteResultCheckGroup", "Результаты голосования (выбор)", voteResultGroup, false);
        voteResultCommentGroup = addAbstractGroup("voteResultCommentGroup", "Результаты голосования (комментарии)", voteResultGroup, false);
    }

    public LP nameNative;
    public LP nameForeign;
    public LP firmNameNativeClaimer;
    public LP firmNameForeignClaimer;
    public LP phoneClaimer;
    public LP addressClaimer;
    public LP siteClaimer;
    public LP emailClaimer;
    LP statementClaimer, loadStatementClaimer, openStatementClaimer;
    LP constituentClaimer, loadConstituentClaimer, openConstituentClaimer;
    LP extractClaimer, loadExtractClaimer, openExtractClaimer;
    public LP OGRNClaimer;
    public LP INNClaimer;
    LP projectVote, claimerVote, nameNativeProjectVote, nameForeignProjectVote;
    LP dataDocumentNameExpert, documentNameExpert;
    LP clusterExpert, nameNativeClusterExpert;
    LP primClusterExpert, extraClusterExpert, inClusterExpert;
    LP clusterInExpertVote;
    public LP clusterProject, nameNativeClusterProject, nameForeignClusterProject;
    LP clusterVote, nameNativeClusterVote;
    LP clusterProjectVote, equalsClusterProjectVote;
    public LP claimerProject;
    LP nameNativeClaimerProject;
    LP nameForeignClaimerProject;
    LP nameNativeJoinClaimerProject;
    LP nameForeignJoinClaimerProject;
    LP emailDocuments;

    LP emailToExpert;

    LP nameNativeClaimerVote, nameForeignClaimerVote;
    public LP nameNativeGenitiveManagerProject;
    LP nameGenitiveManagerProject;
    public LP nameNativeDativusManagerProject;
    LP nameDativusManagerProject;
    public LP nameNativeAblateManagerProject;
    LP nameAblateManagerProject;

    public LP nameNativeClaimer;
    public LP nameForeignClaimer;
    LP nameGenitiveClaimerProject;
    LP nameDativusClaimerProject;
    LP nameAblateClaimerProject;
    LP nameGenitiveClaimerVote;
    LP nameDativusClaimerVote;
    LP nameAblateClaimerVote;

    LP documentTemplateDocumentTemplateDetail;

    LP projectDocument, nameNativeProjectDocument;
    LP fileDocument;
    LP loadFileDocument;
    LP openFileDocument;

    LP inDefaultDocumentLanguage;
    LP inDefaultDocumentExpert;
    LP inDocumentLanguage;
    LP inDocumentExpert;

    LP inExpertVote, oldExpertVote, inNewExpertVote, inOldExpertVote;
    LP dateStartVote, dateEndVote;
    LP aggrDateEndVote;

    LP openedVote;
    LP closedVote;
    LP voteInProgressProject;
    LP requiredPeriod;
    LP requiredQuantity;
    LP limitExperts;
    LP voteStartFormVote;
    LP voteProtocolFormVote;

    LP dateExpertVote;
    LP voteResultExpertVote, nameVoteResultExpertVote;
    LP voteResultNewExpertVote;
    LP inProjectExpert;
    LP voteProjectExpert;
    LP voteResultProjectExpert;
    LP doneProjectExpert;
    LP doneExpertVote, doneNewExpertVote, doneOldExpertVote;
    LP refusedExpertVote;
    LP connectedExpertVote;
    LP expertVoteConnected;
    LP inClusterExpertVote, inClusterNewExpertVote;
    LP innovativeExpertVote, innovativeNewExpertVote;
    LP innovativeCommentExpertVote;
    LP foreignExpertVote, foreignNewExpertVote;
    LP competentExpertVote;
    LP completeExpertVote;
    LP completeCommentExpertVote;

    LP quantityInVote;
    LP quantityRepliedVote;
    LP quantityDoneVote;
    LP quantityDoneNewVote;
    LP quantityDoneOldVote;
    LP quantityRefusedVote;
    LP quantityConnectedVote;
    LP quantityInClusterVote;
    LP quantityInnovativeVote;
    LP quantityForeignVote;
    LP acceptedInClusterVote;
    LP acceptedInnovativeVote;
    LP acceptedForeignVote;

    LP acceptedVote;
    LP succeededVote, succeededClusterVote;
    LP doneExpertVoteDateFromDateTo;
    LP quantityDoneExpertDateFromDateTo;
    LP voteSucceededProject;
    LP noCurrentVoteProject;
    LP voteValuedProject;
    LP acceptedProject;
    LP voteRejectedProject;
    LP needExtraVoteProject;

    LP emailLetterExpertVoteEA, emailLetterExpertVote;
    LP allowedEmailLetterExpertVote;

    LP emailClaimerFromAddress;
    LP emailClaimerVoteEA;
    LP claimerEmailVote;

    LP emailStartVoteEA, emailStartHeaderVote, emailStartVote;
    LP emailProtocolVoteEA, emailProtocolHeaderVote, emailProtocolVote;
    LP emailClosedVoteEA, emailClosedHeaderVote, emailClosedVote;
    LP emailAuthExpertEA, emailAuthExpert;
    LP authExpertSubjectLanguage, letterExpertSubjectLanguage;

    LP generateDocumentsProjectDocumentType;
    LP includeDocumentsProjectDocumentType;
    LP importProjectsAction;
    LP generateVoteProject, hideGenerateVoteProject;
    LP copyResultsVote;

    LP expertLogin;

    LP statusProject, nameStatusProject;
    LP statusProjectVote, nameStatusProjectVote;

    LP projectSucceededClaimer;

    LP quantityTotalExpert;
    LP quantityDoneExpert;
    LP percentDoneExpert;
    LP percentInClusterExpert;
    LP percentInnovativeExpert;
    LP percentForeignExpert;

    LP prevDateStartVote, prevDateVote;
    LP dateProjectVote;
    LP numberNewExpertVote;
    LP numberOldExpertVote;

    LP languageExpert;
    LP nameLanguageExpert;
    LP languageDocument;
    LP nameLanguageDocument;
    LP englishDocument;
    LP defaultEnglishDocumentType;
    LP defaultEnglishDocument;
    LP typeDocument;
    LP nameTypeDocument;
    LP postfixDocument;
    LP hidePostfixDocument;

    LP localeLanguage;

    LP quantityMinLanguageDocumentType;
    LP quantityMaxLanguageDocumentType;
    LP quantityProjectLanguageDocumentType;
    LP translateLanguageDocumentType;
    LP notEnoughProject;
    LP autoGenerateProject;

    LP nameDocument;

    public LP nameNativeProject;
    public LP nameForeignProject;
    public LP nameNativeManagerProject;
    public LP nameForeignManagerProject;
    public LP nativeProblemProject;
    public LP foreignProblemProject;
    public LP nativeInnovativeProject;
    public LP foreignInnovativeProject;
    public LP projectTypeProject;
    LP nameProjectTypeProject;
    public LP nativeSubstantiationProjectType;
    public LP foreignSubstantiationProjectType;
    public LP nativeSubstantiationClusterProject;
    public LP foreignSubstantiationClusterProject;
    LP fileNativeSummaryProject;
    LP loadFileNativeSummaryProject;
    LP openFileNativeSummaryProject;
    LP fileForeignSummaryProject;
    LP loadFileForeignSummaryProject;
    LP openFileForeignSummaryProject;
    LP fileRoadMapProject;
    LP loadFileRoadMapProject;
    LP openFileRoadMapProject;
    LP fileNativeTechnicalDescriptionProject;
    LP loadFileNativeTechnicalDescriptionProject;
    LP openFileNativeTechnicalDescriptionProject;
    LP fileForeignTechnicalDescriptionProject;
    LP loadFileForeignTechnicalDescriptionProject;
    LP openFileForeignTechnicalDescriptionProject;

    public LP isReturnInvestmentsProject;
    public LP nameReturnInvestorProject;
    public LP amountReturnFundsProject;
    public LP isNonReturnInvestmentsProject;
    public LP nameNonReturnInvestorProject;
    public LP amountNonReturnFundsProject;
    public LP isCapitalInvestmentProject;
    public LP isPropertyInvestmentProject;
    public LP isGrantsProject;
    public LP isOtherNonReturnInvestmentsProject;
    public LP commentOtherNonReturnInvestmentsProject;
    public LP isOwnFundsProject;
    public LP amountOwnFundsProject;
    public LP isPlanningSearchSourceProject;
    public LP amountFundsProject;
    public LP isOtherSoursesProject;
    public LP commentOtherSoursesProject;
    LP hideNameReturnInvestorProject;
    LP hideAmountReturnFundsProject;
    LP hideNameNonReturnInvestorProject;
    LP hideAmountNonReturnFundsProject;
    LP hideCommentOtherNonReturnInvestmentsProject;
    LP hideAmountOwnFundsProject;
    LP hideAmountFundsProject;
    LP hideCommentOtherSoursesProject;
    LP hideIsCapitalInvestmentProject;
    LP hideIsPropertyInvestmentProject;
    LP hideIsGrantsProject;
    LP hideIsOtherNonReturnInvestmentsProject;

    public LP isOwnedEquipmentProject;
    public LP isAvailableEquipmentProject;
    public LP isTransferEquipmentProject;
    public LP descriptionTransferEquipmentProject;
    public LP ownerEquipmentProject;
    public LP isPlanningEquipmentProject;
    public LP specificationEquipmentProject;
    public LP isSeekEquipmentProject;
    public LP descriptionEquipmentProject;
    public LP isOtherEquipmentProject;
    public LP commentEquipmentProject;
    LP hideDescriptionTransferEquipmentProject;
    LP hideOwnerEquipmentProject;
    LP hideSpecificationEquipmentProject;
    LP hideDescriptionEquipmentProject;
    LP hideCommentEquipmentProject;

    public LP projectPatent;
    public LP nativeTypePatent;
    public LP foreignTypePatent;
    public LP nativeNumberPatent;
    public LP foreignNumberPatent;
    public LP priorityDatePatent;
    public LP isOwned;
    public LP ownerPatent;
    public LP ownerTypePatent;
    LP ownerTypeToPatent;
    LP nameOwnerTypePatent;
    public LP ownerTypeToSID;
    public LP projectTypeToSID;
    LP fileIntentionOwnerPatent;
    LP loadFileIntentionOwnerPatent;
    LP openFileIntentionOwnerPatent;
    public LP isValuated;
    public LP valuatorPatent;
    LP fileActValuationPatent;
    LP loadFileActValuationPatent;
    LP openFileActValuationPatent;
    LP hideOwnerPatent;
    LP hideFileIntentionOwnerPatent;
    LP hideLoadFileIntentionOwnerPatent;
    LP hideOpenFileIntentionOwnerPatent;
    LP hideValuatorPatent;
    LP hideFileActValuationPatent;
    LP hideLoadFileActValuationPatent;
    LP hideOpenFileActValuationPatent;
    LP hideNameOwnerTypePatent;

    public LP projectAcademic;
    public LP fullNameAcademic;
    public LP fullNameToAcademic;
    public LP institutionAcademic;
    public LP titleAcademic;
    LP fileDocumentConfirmingAcademic;
    LP loadFileDocumentConfirmingAcademic;
    LP openFileDocumentConfirmingAcademic;
    LP fileDocumentEmploymentAcademic;
    LP loadFileDocumentEmploymentAcademic;
    LP openFileDocumentEmploymentAcademic;

    public LP projectNonRussianSpecialist;
    public LP fullNameNonRussianSpecialist;
    public LP fullNameToNonRussianSpecialist;
    public LP organizationNonRussianSpecialist;
    public LP titleNonRussianSpecialist;
    LP fileNativeResumeNonRussianSpecialist;
    LP loadFileForeignResumeNonRussianSpecialist;
    LP openFileForeignResumeNonRussianSpecialist;
    LP fileForeignResumeNonRussianSpecialist;
    LP loadFileNativeResumeNonRussianSpecialist;
    LP openFileNativeResumeNonRussianSpecialist;
    LP filePassportNonRussianSpecialist;
    LP loadFilePassportNonRussianSpecialist;
    LP openFilePassportNonRussianSpecialist;
    LP fileStatementNonRussianSpecialist;
    LP loadFileStatementNonRussianSpecialist;
    LP openFileStatementNonRussianSpecialist;

    LP isForeignExpert;
    LP localeExpert;
    LP disableExpert;

    LP editClaimer;
    LP addProject, editProject;

    @Override
    public void initProperties() {
        idGroup.add(baseLM.objectValue);

        nameNative = addDProp(recognizeGroup, "nameNative", "Имя", InsensitiveStringClass.get(2000), multiLanguageNamed);
        nameNative.setMinimumWidth(10); nameNative.setPreferredWidth(50);
        nameForeign = addDProp(recognizeGroup, "nameForeign", "Имя (иностр.)", InsensitiveStringClass.get(2000), multiLanguageNamed);
        nameForeign.setMinimumWidth(10); nameForeign.setPreferredWidth(50);

        baseGroup.add(baseLM.email.property); // сделано, чтобы email был не самой первой колонкой в диалогах

        LP percent = addSFProp("(prm1*100/prm2)", DoubleClass.instance, 2);

        // глобальные свойства
        requiredPeriod = addDProp(baseGroup, "votePeriod", "Срок заседания", IntegerClass.instance);
        requiredQuantity = addDProp(baseGroup, "voteRequiredQuantity", "Кол-во экспертов", IntegerClass.instance);
        limitExperts = addDProp(baseGroup, "limitExperts", "Кол-во прогол. экспертов", IntegerClass.instance);

        //свойства заявителя
        nameNativeClaimer = addJProp(claimerInformationGroup, "nameNativeClaimer", "Заявитель", baseLM.and1, nameNative, 1, is(claimer), 1);
        nameNativeClaimer.setMinimumWidth(10); nameNativeClaimer.setPreferredWidth(50);
        nameNativeToClaimer = addAGProp("nameNativeToClaimer", "Имя заявителя", nameNativeClaimer);
        nameForeignClaimer = addJProp(claimerInformationGroup, "nameForeignClaimer", "Claimer", baseLM.and1,  nameForeign, 1, is(claimer), 1);
        nameForeignClaimer.setMinimumWidth(10); nameForeignClaimer.setPreferredWidth(50);
        firmNameNativeClaimer = addDProp(claimerInformationGroup, "firmNameNativeClaimer", "Фирменное название", InsensitiveStringClass.get(2000), claimer);
        firmNameNativeClaimer.setMinimumWidth(10); firmNameNativeClaimer.setPreferredWidth(50);
        firmNameForeignClaimer = addDProp(claimerInformationGroup, "firmNameForeignClaimer", "Brand name", InsensitiveStringClass.get(2000), claimer);
        firmNameForeignClaimer.setMinimumWidth(10); firmNameForeignClaimer.setPreferredWidth(50);
        phoneClaimer = addDProp(contactGroup, "phoneClaimer", "Телефон", StringClass.get(100), claimer);
        addressClaimer = addDProp(contactGroup, "addressClaimer", "Адрес", StringClass.get(2000), claimer);
        addressClaimer.setMinimumWidth(10); addressClaimer.setPreferredWidth(50);
        siteClaimer = addDProp(contactGroup, "siteClaimer", "Сайт", StringClass.get(100), claimer);
        emailClaimer = addJProp(contactGroup, "emailClaimer", "E-mail", baseLM.and1, baseLM.email, 1, is(claimer), 1);

        statementClaimer = addDProp("statementClaimer", "Заявление", PDFClass.instance, claimer);
        loadStatementClaimer = addLFAProp(documentGroup, "Загрузить заявление", statementClaimer);
        openStatementClaimer = addOFAProp(documentGroup, "Открыть заявление", statementClaimer);

        constituentClaimer = addDProp("constituentClaimer", "Учредительные документы", PDFClass.instance, claimer);
        loadConstituentClaimer = addLFAProp(documentGroup, "Загрузить учредительные документы", constituentClaimer);
        openConstituentClaimer = addOFAProp(documentGroup, "Открыть учредительные документы", constituentClaimer);

        extractClaimer = addDProp("extractClaimer", "Выписка из реестра", PDFClass.instance, claimer);
        loadExtractClaimer = addLFAProp(documentGroup, "Загрузить выписку из реестра", extractClaimer);
        openExtractClaimer = addOFAProp(documentGroup, "Открыть Выписку из реестра", extractClaimer);

        OGRNClaimer = addDProp(legalDataGroup, "OGRNClaimer", "ОГРН", StringClass.get(13), claimer);
        INNClaimer = addDProp(legalDataGroup, "INNClaimer", "ИНН", StringClass.get(12), claimer);

        projectVote = addDProp(idGroup, "projectVote", "Проект (ИД)", project, vote);
        setNotNull(projectVote);

        nameNativeProjectVote = addJProp(baseGroup, "nameNativeProjectVote", "Проект", nameNative, projectVote, 1);
        nameForeignProjectVote = addJProp(baseGroup, "nameForeignProjectVote", "Проект (иностр.)", nameForeign, projectVote, 1);

        dateProjectVote = addJProp("dateProjectVote", "Дата проекта", baseLM.date, projectVote, 1);

        dataDocumentNameExpert = addDProp("dataDocumentNameExpert", "Имя для документов", StringClass.get(70), expert);
        documentNameExpert = addSUProp("documentNameExpert", "Имя для документов", Union.OVERRIDE, addJProp(baseLM.and1, addJProp(baseLM.insensitiveString2, baseLM.userLastName, 1, baseLM.userFirstName, 1), 1, is(expert), 1), dataDocumentNameExpert);

        clusterExpert = addDProp(idGroup, "clusterExpert", "Кластер (ИД)", cluster, expert);
        nameNativeClusterExpert = addJProp(baseGroup, "nameNativeClusterExpert", "Кластер", nameNative, clusterExpert, 1);

        primClusterExpert = addJProp("primClusterExpert", "Вкл (осн.)", baseLM.equals2, 1, clusterExpert, 2);
        extraClusterExpert = addDProp(baseGroup, "extraClusterExpert", "Вкл (доп.)", LogicalClass.instance, cluster, expert);
        inClusterExpert = addSUProp(baseGroup, "inClusterExpert", true, "Вкл", Union.OVERRIDE, extraClusterExpert, addJProp(baseLM.equals2, 1, clusterExpert, 2));

        // project
        claimerProject = addDProp(idGroup, "claimerProject", "Заявитель (ИД)", claimer, project);

        claimerVote = addJProp(idGroup, "claimerVote", "Заявитель (ИД)", claimerProject, projectVote, 1);

        sidProject = addDProp(projectInformationGroup, "sidProject", "Внешний идентификатор проекта", StringClass.get(10), project);
        sidToProject = addAGProp("sidToProject", "SID проекта", sidProject);
        nameNativeManagerProject = addDProp(projectInformationGroup, "nameNativeManagerProject", "ФИО руководителя проекта", InsensitiveStringClass.get(2000), project);
        nameNativeManagerProject.setMinimumWidth(10); nameNativeManagerProject.setPreferredWidth(50);
        nameNativeGenitiveManagerProject = addDProp(projectInformationGroup, "nameNativeGenitiveManagerProject", "ФИО руководителя проекта (Кого)", InsensitiveStringClass.get(2000), project);
        nameNativeGenitiveManagerProject.setMinimumWidth(10); nameNativeGenitiveManagerProject.setPreferredWidth(50);
        nameGenitiveManagerProject = addSUProp("nameGenitiveManagerProject", "Заявитель (Кого)", Union.OVERRIDE, nameNativeManagerProject, nameNativeGenitiveManagerProject);
        nameGenitiveManagerProject.setMinimumWidth(10); nameGenitiveManagerProject.setPreferredWidth(50);
        nameNativeDativusManagerProject = addDProp(projectInformationGroup, "nameNativeDativusManagerProject", "ФИО руководителя проекта (Кому)", InsensitiveStringClass.get(2000), project);
        nameNativeDativusManagerProject.setMinimumWidth(10); nameNativeDativusManagerProject.setPreferredWidth(50);
        nameDativusManagerProject = addSUProp("nameDativusManagerProject", "Заявитель (Кому)", Union.OVERRIDE, nameNativeManagerProject, nameNativeDativusManagerProject);
        nameDativusManagerProject.setMinimumWidth(10); nameDativusManagerProject.setPreferredWidth(50);
        nameNativeAblateManagerProject = addDProp(projectInformationGroup, "nameNativeAblateManagerProject", "ФИО руководителя проекта (Кем)", InsensitiveStringClass.get(2000), project);
        nameNativeAblateManagerProject.setMinimumWidth(10); nameNativeAblateManagerProject.setPreferredWidth(50);
        nameAblateManagerProject = addSUProp("nameAblateManagerProject", "Заявитель (Кем)", Union.OVERRIDE, nameNativeManagerProject, nameNativeAblateManagerProject);
        nameAblateManagerProject.setMinimumWidth(10); nameAblateManagerProject.setPreferredWidth(50);
        nameForeignManagerProject = addDProp(projectInformationGroup, "nameForeignManagerProject", "Full name project manager", InsensitiveStringClass.get(2000), project);
        nameForeignManagerProject.setMinimumWidth(10); nameForeignManagerProject.setPreferredWidth(50);

        nameNativeJoinClaimerProject = addJProp(projectInformationGroup, "nameNativeJoinClaimerProject", "Заявитель", nameNative, claimerProject, 1);
        nameNativeClaimerProject = addSUProp("nameNativeClaimerProject", "Заявитель", Union.OVERRIDE, nameNativeManagerProject, nameNativeJoinClaimerProject);
        nameNativeClaimerProject.setMinimumWidth(10); nameNativeClaimerProject.setPreferredWidth(50);
        nameForeignJoinClaimerProject = addJProp(projectInformationGroup, "nameForeignJoinClaimerProject", "Claimer", nameForeign, claimerProject, 1);
        nameForeignClaimerProject = addSUProp("nameForeignClaimerProject", "Claimer", Union.OVERRIDE, nameForeignManagerProject, nameForeignJoinClaimerProject);
        nameForeignClaimerProject.setMinimumWidth(10); nameForeignClaimerProject.setPreferredWidth(50);
        nameGenitiveClaimerProject = addSUProp(additionalInformationGroup, "nameGenitiveClaimerProject", "Заявитель (Кого)", Union.OVERRIDE, nameGenitiveManagerProject, nameNativeJoinClaimerProject);
        nameGenitiveClaimerProject.setMinimumWidth(10); nameGenitiveClaimerProject.setPreferredWidth(50);
        nameDativusClaimerProject = addSUProp(additionalInformationGroup, "nameDativusClaimerProject", "Заявитель (Кому)", Union.OVERRIDE, nameDativusManagerProject, nameNativeJoinClaimerProject);
        nameDativusClaimerProject.setMinimumWidth(10); nameDativusClaimerProject.setPreferredWidth(50);
        nameAblateClaimerProject = addSUProp(additionalInformationGroup, "nameAblateClaimerProject", "Заявитель (Кем)", Union.OVERRIDE, nameAblateManagerProject, nameNativeJoinClaimerProject);
        nameAblateClaimerProject.setMinimumWidth(10); nameAblateClaimerProject.setPreferredWidth(50);

        nameNativeProject = addJProp(projectInformationGroup, "nameNativeProject", "Название проекта", baseLM.and1, nameNative, 1, is(project), 1);
        nameNativeProject.setMinimumWidth(10); nameNativeProject.setPreferredWidth(50);
        nameForeignProject = addJProp(projectInformationGroup, "nameForeignProject", "Name of the project", baseLM.and1,  nameForeign, 1, is(project), 1);
        nameForeignProject.setMinimumWidth(10); nameForeignProject.setPreferredWidth(50);

        nativeProblemProject = addDProp(innovationGroup, "nativeProblemProject", "Проблема, на решение которой направлен проект", InsensitiveStringClass.get(2000), project);
        nativeProblemProject.setMinimumWidth(10); nativeProblemProject.setPreferredWidth(50);
        foreignProblemProject = addDProp(innovationGroup, "foreignProblemProject", "The problem that the project will solve", InsensitiveStringClass.get(2000), project);
        foreignProblemProject.setMinimumWidth(10); foreignProblemProject.setPreferredWidth(50);

        nativeInnovativeProject = addDProp(innovationGroup, "nativeInnovativeProject", "Суть инновации", InsensitiveStringClass.get(2000), project);
        nativeInnovativeProject.setMinimumWidth(10); nativeInnovativeProject.setPreferredWidth(50);
        foreignInnovativeProject = addDProp(innovationGroup, "foreignInnovativeProject", "Description of the innovation", InsensitiveStringClass.get(2000), project);
        foreignInnovativeProject.setMinimumWidth(10); foreignInnovativeProject.setPreferredWidth(50);

        projectTypeProject = addDProp(idGroup, "projectTypeProject", "Тип проекта (ИД)", projectType, project);
        nameProjectTypeProject = addJProp(innovationGroup, "nameProjectTypeProject", "Тип проекта", baseLM.name, projectTypeProject, 1);
        nativeSubstantiationProjectType = addDProp(innovationGroup, "nativeSubstantiationProjectType", "Обоснование выбора", InsensitiveStringClass.get(2000), project);
        nativeSubstantiationProjectType.setMinimumWidth(10); nativeSubstantiationProjectType.setPreferredWidth(50);
        foreignSubstantiationProjectType = addDProp(innovationGroup, "foreignSubstantiationProjectType", "Description of choice", InsensitiveStringClass.get(2000), project);
        foreignSubstantiationProjectType.setMinimumWidth(10); foreignSubstantiationProjectType.setPreferredWidth(50);

        clusterProject = addDProp(idGroup, "clusterProject", "Кластер (ИД)", cluster, project);
        nameNativeCluster = addJProp("nameNativeCluster", "Кластер", baseLM.and1, nameNative, 1, is(cluster), 1);
        nameNativeToCluster = addAGProp(idGroup, "nameNativeToCluster", "Кластер", nameNativeCluster);
        nameForeignCluster = addJProp("nameForeignCluster", "Cluster", baseLM.and1,  nameForeign, 1, is(cluster), 1);
        nameNativeClusterProject = addJProp(innovationGroup, "nameNativeClusterProject", "Кластер", nameNative, clusterProject, 1);
        nameForeignClusterProject = addJProp(innovationGroup, "nameForeignClusterProject", "Кластер (иностр.)", nameForeign, clusterProject, 1);
        nativeSubstantiationClusterProject = addDProp(innovationGroup, "nativeSubstantiationClusterProject", "Обоснование выбора", InsensitiveStringClass.get(2000), project);
        nativeSubstantiationClusterProject.setMinimumWidth(10); nativeSubstantiationClusterProject.setPreferredWidth(50);
        foreignSubstantiationClusterProject = addDProp(innovationGroup, "foreignSubstantiationClusterProject", "Description of choice", InsensitiveStringClass.get(2000), project);
        foreignSubstantiationClusterProject.setMinimumWidth(10); foreignSubstantiationClusterProject.setPreferredWidth(50);

        fileNativeSummaryProject = addDProp("fileNativeSummaryProject", "Файл резюме проекта", PDFClass.instance, project);
        loadFileNativeSummaryProject = addLFAProp(executiveSummaryGroup, "Загрузить файл резюме проекта", fileNativeSummaryProject);
        openFileNativeSummaryProject = addOFAProp(executiveSummaryGroup, "Открыть файл резюме проекта", fileNativeSummaryProject);

        fileForeignSummaryProject = addDProp("fileForeignSummaryProject", "Файл резюме проекта (иностр.)", PDFClass.instance, project);
        loadFileForeignSummaryProject = addLFAProp(executiveSummaryGroup, "Загрузить файл резюме проекта (иностр.)", fileForeignSummaryProject);
        openFileForeignSummaryProject = addOFAProp(executiveSummaryGroup, "Открыть файл резюме проекта (иностр.)", fileForeignSummaryProject);

        // источники финансирования
        isReturnInvestmentsProject = addDProp(sourcesFundingGroup, "isReturnInvestmentsProject", "Средства третьих лиц, привлекаемые на возвратной основе (заемные средства и т.п.)", LogicalClass.instance, project);
        nameReturnInvestorProject = addDProp(sourcesFundingGroup, "nameReturnInvestorProject", "Третьи лица для возврата средств", InsensitiveStringClass.get(2000), project);
        nameReturnInvestorProject.setMinimumWidth(10); nameReturnInvestorProject.setPreferredWidth(50);
        amountReturnFundsProject = addDProp(sourcesFundingGroup, "amountReturnFundsProject", "Объем средств на возвратной основе (тыс. руб.)", DoubleClass.instance, project);
        hideNameReturnInvestorProject = addHideCaptionProp(privateGroup, "укажите данных лиц и их контактную информацию", nameReturnInvestorProject, isReturnInvestmentsProject);
        hideAmountReturnFundsProject = addHideCaptionProp(privateGroup, "укажите объем привлекаемых средств (тыс. руб.)", amountReturnFundsProject, isReturnInvestmentsProject);

        isNonReturnInvestmentsProject = addDProp(sourcesFundingGroup, "isNonReturnInvestmentsProject", "Средства третьих лиц, привлекаемые на безвозвратной основе (гранты и т.п.)", LogicalClass.instance, project);

        isCapitalInvestmentProject = addDProp(sourcesFundingGroup, "isCapitalInvestmentProject", "Вклады в уставный капитал", LogicalClass.instance, project);
        isPropertyInvestmentProject = addDProp(sourcesFundingGroup, "isPropertyInvestmentProject", "Вклады в имущество", LogicalClass.instance, project);
        isGrantsProject = addDProp(sourcesFundingGroup, "isGrantsProject", "Гранты", LogicalClass.instance, project);

        isOtherNonReturnInvestmentsProject = addDProp(sourcesFundingGroup, "isOtherNonReturnInvestmentsProject", "Иное", LogicalClass.instance, project);

        nameNonReturnInvestorProject = addDProp(sourcesFundingGroup, "nameNonReturnInvestorProject", "Третьи лица для возврата средств", InsensitiveStringClass.get(2000), project);
        nameNonReturnInvestorProject.setMinimumWidth(10); nameNonReturnInvestorProject.setPreferredWidth(50);
        amountNonReturnFundsProject = addDProp(sourcesFundingGroup, "amountNonReturnFundsProject", "Объем средств на безвозвратной основе (тыс. руб.)", DoubleClass.instance, project);
        hideNameNonReturnInvestorProject = addHideCaptionProp(privateGroup, "укажите данных лиц и их контактную информацию", nameReturnInvestorProject, isNonReturnInvestmentsProject);
        hideAmountNonReturnFundsProject = addHideCaptionProp(privateGroup, "укажите объем привлекаемых средств (тыс. руб.)", amountReturnFundsProject, isNonReturnInvestmentsProject);

        commentOtherNonReturnInvestmentsProject = addDProp(sourcesFundingGroup, "commentOtherNonReturnInvestmentsProject", "Комментарий", InsensitiveStringClass.get(2000), project);
        commentOtherNonReturnInvestmentsProject.setMinimumWidth(10); commentOtherNonReturnInvestmentsProject.setPreferredWidth(50);

        hideIsCapitalInvestmentProject = addHideCaptionProp(privateGroup, "Укажите", isCapitalInvestmentProject, isNonReturnInvestmentsProject);
        hideIsPropertyInvestmentProject = addHideCaptionProp(privateGroup, "Укажите", isPropertyInvestmentProject, isNonReturnInvestmentsProject);
        hideIsGrantsProject = addHideCaptionProp(privateGroup, "Укажите", isGrantsProject, isNonReturnInvestmentsProject);
        hideIsOtherNonReturnInvestmentsProject = addHideCaptionProp(privateGroup, "Укажите", isOtherNonReturnInvestmentsProject, isNonReturnInvestmentsProject);

        hideCommentOtherNonReturnInvestmentsProject = addHideCaptionProp(privateGroup, "Укажите", commentOtherNonReturnInvestmentsProject, isOtherNonReturnInvestmentsProject);

        isOwnFundsProject = addDProp(sourcesFundingGroup, "isOwnFundsProject", "Собственные средства организации", LogicalClass.instance, project);
        amountOwnFundsProject = addDProp(sourcesFundingGroup, "amountOwnFundsProject", "Объем собственных средств (тыс. руб.)", DoubleClass.instance, project);
        hideAmountOwnFundsProject = addHideCaptionProp(privateGroup, "Укажите", amountOwnFundsProject, isOwnFundsProject);

        isPlanningSearchSourceProject = addDProp(sourcesFundingGroup, "isPlanningSearchSourceProject", "Планируется поиск источника финансирования проекта", LogicalClass.instance, project);
        amountFundsProject = addDProp(sourcesFundingGroup, "amountFundsProject", "Требуемый объем средств (тыс. руб.)", DoubleClass.instance, project);
        hideAmountFundsProject = addHideCaptionProp(privateGroup, "Укажите", amountFundsProject, isPlanningSearchSourceProject);

        isOtherSoursesProject = addDProp(sourcesFundingGroup, "isOtherSoursesProject", "Иное", LogicalClass.instance, project);
        commentOtherSoursesProject = addDProp(sourcesFundingGroup, "commentOtherSoursesProject", "Комментарий", InsensitiveStringClass.get(2000), project);
        commentOtherSoursesProject.setMinimumWidth(10); commentOtherSoursesProject.setPreferredWidth(50);
        hideCommentOtherSoursesProject = addHideCaptionProp(privateGroup, "Укажите", commentOtherSoursesProject, isOtherSoursesProject);

        // оборудование
        isOwnedEquipmentProject = addDProp(equipmentGroup, "isOwnedEquipmentProject", "Оборудование имеется в собственности и/или в пользовании Вашей организации", LogicalClass.instance, project);

        isAvailableEquipmentProject = addDProp(equipmentGroup, "isAvailableEquipmentProject", "Оборудование имеется в открытом доступе на рынке, и Ваша организация планирует приобрести его в собственность и/или в пользование", LogicalClass.instance, project);

        isTransferEquipmentProject = addDProp(equipmentGroup, "isTransferEquipmentProject", "Оборудование отсутствует в открытом доступе на рынке, но достигнута договоренность с собственником оборудования о передаче данного оборудования в собственность и/или в пользование для реализации проекта", LogicalClass.instance, project);
        descriptionTransferEquipmentProject = addDProp(equipmentGroup, "descriptionTransferEquipmentProject", "Опишите данное оборудование", InsensitiveStringClass.get(2000), project);
        descriptionTransferEquipmentProject.setMinimumWidth(10); descriptionTransferEquipmentProject.setPreferredWidth(50);
        ownerEquipmentProject = addDProp(equipmentGroup, "ownerEquipmentProject", "Укажите собственника оборудования и его контактную информацию", InsensitiveStringClass.get(2000), project);
        ownerEquipmentProject.setMinimumWidth(10); ownerEquipmentProject.setPreferredWidth(50);
        hideDescriptionTransferEquipmentProject = addHideCaptionProp(privateGroup, "Укажите", descriptionTransferEquipmentProject, isTransferEquipmentProject);
        hideOwnerEquipmentProject = addHideCaptionProp(privateGroup, "Укажите", ownerEquipmentProject, isTransferEquipmentProject);

        isPlanningEquipmentProject = addDProp(equipmentGroup, "isPlanningEquipmentProject", "Ваша организация планирует использовать для реализации проекта оборудование, которое имеется в собственности или в пользовании Фонда «Сколково» (учрежденных им юридических лиц)", LogicalClass.instance, project);
        specificationEquipmentProject = addDProp(equipmentGroup, "specificationEquipmentProject", "Укажите данное оборудование", InsensitiveStringClass.get(2000), project);
        specificationEquipmentProject.setMinimumWidth(10); specificationEquipmentProject.setPreferredWidth(50);
        hideSpecificationEquipmentProject = addHideCaptionProp(privateGroup, "Укажите", specificationEquipmentProject, isPlanningEquipmentProject);

        isSeekEquipmentProject = addDProp(equipmentGroup, "isSeekEquipmentProject", "Оборудование имеется на рынке, но Ваша организация не имеет возможности приобрести его в собственность или в пользование и ищет возможность получить доступ к такому оборудованию", LogicalClass.instance, project);
        descriptionEquipmentProject = addDProp(equipmentGroup, "descriptionEquipmentProject", "Опишите данное оборудование", InsensitiveStringClass.get(2000), project);
        descriptionEquipmentProject.setMinimumWidth(10); descriptionEquipmentProject.setPreferredWidth(50);
        hideDescriptionEquipmentProject = addHideCaptionProp(privateGroup, "Укажите", descriptionEquipmentProject, isSeekEquipmentProject);

        isOtherEquipmentProject = addDProp(equipmentGroup, "isOtherEquipmentProject", "Иное", LogicalClass.instance, project);
        commentEquipmentProject = addDProp(equipmentGroup, "commentEquipmentProject", "Комментарий", InsensitiveStringClass.get(2000), project);
        commentEquipmentProject.setMinimumWidth(10); commentEquipmentProject.setPreferredWidth(50);
        hideCommentEquipmentProject = addHideCaptionProp(privateGroup, "Укажите", commentEquipmentProject, isOtherEquipmentProject);

        // документы
        fileRoadMapProject = addDProp("fileRoadMapProject", "Файл дорожной карты", PDFClass.instance, project);
        loadFileRoadMapProject = addLFAProp(projectDocumentsGroup, "Загрузить файл дорожной карты", fileRoadMapProject);
        openFileRoadMapProject = addOFAProp(projectDocumentsGroup, "Открыть файл дорожной карты", fileRoadMapProject);

        fileNativeTechnicalDescriptionProject = addDProp("fileNativeTechnicalDescriptionProject", "Файл технического описания", PDFClass.instance, project);
        loadFileNativeTechnicalDescriptionProject = addLFAProp(projectDocumentsGroup, "Загрузить файл технического описания", fileNativeTechnicalDescriptionProject);
        openFileNativeTechnicalDescriptionProject = addOFAProp(projectDocumentsGroup, "Открыть файл технического описания", fileNativeTechnicalDescriptionProject);

        fileForeignTechnicalDescriptionProject = addDProp("fileForeignTechnicalDescriptionProject", "Файл технического описания (иностр.)", PDFClass.instance, project);
        loadFileForeignTechnicalDescriptionProject = addLFAProp(projectDocumentsGroup, "Загрузить файл технического описания (иностр.)", fileForeignTechnicalDescriptionProject);
        openFileForeignTechnicalDescriptionProject = addOFAProp(projectDocumentsGroup, "Открыть файл технического описания (иностр.)", fileForeignTechnicalDescriptionProject);

        // патенты
        projectPatent = addDProp(idGroup, "projectPatent", "Проект патента", project, patent);

        nativeTypePatent = addDProp(baseGroup, "nativeTypePatent", "Тип заявки/патента", InsensitiveStringClass.get(2000), patent);
        nativeTypePatent.setMinimumWidth(10); nativeTypePatent.setPreferredWidth(50);
        foreignTypePatent = addDProp(baseGroup, "foreignTypePatent", "Type of application/patent", InsensitiveStringClass.get(2000), patent);
        foreignTypePatent.setMinimumWidth(10); foreignTypePatent.setPreferredWidth(50);
        nativeNumberPatent = addDProp(baseGroup, "nativeNumberPatent", "Номер", InsensitiveStringClass.get(2000), patent);
        nativeNumberPatent.setMinimumWidth(10); nativeNumberPatent.setPreferredWidth(50);
        nativeNumberToPatent = addAGProp("nativeNumberToPatent", "номер патента", nativeNumberPatent);
        foreignNumberPatent = addDProp(baseGroup, "foreignNumberPatent", "Reference number", InsensitiveStringClass.get(2000), patent);
        foreignNumberPatent.setMinimumWidth(10); foreignNumberPatent.setPreferredWidth(50);
        priorityDatePatent = addDProp(baseGroup, "priorityDatePatent", "Дата приоритета", DateClass.instance, patent);

        isOwned = addDProp(baseGroup, "isOwned", "Организация не обладает исключительными правами на указанные результаты интеллектуальной деятельности", LogicalClass.instance, patent);
        ownerPatent = addDProp(baseGroup, "ownerPatent", "Укажите правообладателя и его контактную информацию", InsensitiveStringClass.get(2000), patent);
        ownerPatent.setMinimumWidth(10); ownerPatent.setPreferredWidth(50);
        ownerTypePatent = addDProp(idGroup, "ownerTypePatent", "Кем является правообладатель (ИД)", ownerType, patent);
        ownerTypeToPatent = addAGProp("ownerTypeToPatent", "Кем является правообладатель (ИД)", ownerTypePatent);
        nameOwnerTypePatent = addJProp(baseGroup, "nameOwnerTypePatent", "Кем является правообладатель", baseLM.name, ownerTypePatent, 1);
        ownerTypeToSID = addAGProp("ownerTypeToSID", "SID типа правообладателя", addJProp(baseLM.and1, baseLM.classSID, 1, is(ownerType), 1));
        projectTypeToSID = addAGProp("projectTypeToSID", "SID типа проекта", addJProp(baseLM.and1, baseLM.classSID, 1, is(projectType), 1));
        fileIntentionOwnerPatent = addDProp("fileIntentionOwnerPatent", "Файл документа о передаче права", PDFClass.instance, patent);
        loadFileIntentionOwnerPatent = addLFAProp(baseGroup, "Загрузить файл документа о передаче права", fileIntentionOwnerPatent);
        openFileIntentionOwnerPatent = addOFAProp(baseGroup, "Открыть файл документа о передаче права", fileIntentionOwnerPatent);

        hideOwnerPatent = addHideCaptionProp(privateGroup, "Укажите", ownerPatent, isOwned);
        hideNameOwnerTypePatent = addHideCaptionProp(privateGroup, "Укажите", nameOwnerTypePatent, isOwned);
        hideFileIntentionOwnerPatent = addHideCaptionProp(privateGroup, "Укажите", fileIntentionOwnerPatent, isOwned);
        hideLoadFileIntentionOwnerPatent = addHideCaptionProp(privateGroup, "Укажите", loadFileIntentionOwnerPatent, isOwned);
        hideOpenFileIntentionOwnerPatent = addHideCaptionProp(privateGroup, "Укажите", openFileIntentionOwnerPatent, isOwned);

        isValuated = addDProp(baseGroup, "isValuated", "Проводилась ли оценка указанных результатов интеллектальной деятельности", LogicalClass.instance, patent);
        valuatorPatent = addDProp(baseGroup, "valuatorPatent", "Укажите оценщика и его контактную информацию", InsensitiveStringClass.get(2000), patent);
        valuatorPatent.setMinimumWidth(10); valuatorPatent.setPreferredWidth(50);
        fileActValuationPatent = addDProp("fileActValuationPatent", "Файл акта оценки", PDFClass.instance, patent);
        loadFileActValuationPatent = addLFAProp(baseGroup, "Загрузить файл акта оценки", fileActValuationPatent);
        openFileActValuationPatent = addOFAProp(baseGroup, "Открыть файл акта оценки", fileActValuationPatent);
        hideValuatorPatent = addHideCaptionProp(privateGroup, "Укажите", valuatorPatent, isValuated);
        hideFileActValuationPatent = addHideCaptionProp(privateGroup, "Укажите", fileActValuationPatent, isValuated);
        hideLoadFileActValuationPatent = addHideCaptionProp(privateGroup, "Укажите", loadFileActValuationPatent, isValuated);
        hideOpenFileActValuationPatent = addHideCaptionProp(privateGroup, "Укажите", openFileActValuationPatent, isValuated);

        // учёные
        projectAcademic = addDProp(idGroup, "projectAcademic", "Проект ученого", project, academic);

        fullNameAcademic = addDProp(baseGroup, "fullNameAcademic", "ФИО", InsensitiveStringClass.get(2000), academic);
        fullNameAcademic.setMinimumWidth(10); fullNameAcademic.setPreferredWidth(50);
        fullNameToAcademic = addAGProp("fullNameToAcademic", "ФИО учёного", fullNameAcademic);
        institutionAcademic = addDProp(baseGroup, "institutionAcademic", "Учреждение, в котором данный специалист осуществляет научную и (или) преподавательскую деятельность", InsensitiveStringClass.get(2000), academic);
        institutionAcademic.setMinimumWidth(10); institutionAcademic.setPreferredWidth(50);
        titleAcademic = addDProp(baseGroup, "titleAcademic", "Ученая степень, звание, должность и др.", InsensitiveStringClass.get(2000), academic);
        titleAcademic.setMinimumWidth(10); titleAcademic.setPreferredWidth(50);

        fileDocumentConfirmingAcademic = addDProp("fileDocumentConfirmingAcademic", "Файл трудового договора", PDFClass.instance, academic);
        loadFileDocumentConfirmingAcademic = addLFAProp(baseGroup, "Загрузить файл трудового договора", fileDocumentConfirmingAcademic);
        openFileDocumentConfirmingAcademic = addOFAProp(baseGroup, "Открыть файл трудового договора", fileDocumentConfirmingAcademic);

        fileDocumentEmploymentAcademic = addDProp("fileDocumentEmploymentAcademic", "Файл заявления специалиста", PDFClass.instance, academic);
        loadFileDocumentEmploymentAcademic = addLFAProp(baseGroup, "Загрузить файл заявления", fileDocumentEmploymentAcademic);
        openFileDocumentEmploymentAcademic = addOFAProp(baseGroup, "Открыть файл заявления", fileDocumentEmploymentAcademic);

        // иностранные специалисты
        projectNonRussianSpecialist = addDProp(idGroup, "projectNonRussianSpecialist", "Проект иностранного специалиста", project, nonRussianSpecialist);

        fullNameNonRussianSpecialist = addDProp(baseGroup, "fullNameNonRussianSpecialist", "ФИО", InsensitiveStringClass.get(2000), nonRussianSpecialist);
        fullNameNonRussianSpecialist.setMinimumWidth(10); fullNameNonRussianSpecialist.setPreferredWidth(50);
        fullNameToNonRussianSpecialist = addAGProp("fullNameToNonRussianSpecialist", "ФИО иностранного специалиста", fullNameNonRussianSpecialist);
        organizationNonRussianSpecialist = addDProp(baseGroup, "organizationNonRussianSpecialist", "Место работы", InsensitiveStringClass.get(2000), nonRussianSpecialist);
        organizationNonRussianSpecialist.setMinimumWidth(10); organizationNonRussianSpecialist.setPreferredWidth(50);
        titleNonRussianSpecialist = addDProp(baseGroup, "titleNonRussianSpecialist", "Должность, если есть - ученая степень, звание и др.", InsensitiveStringClass.get(2000), nonRussianSpecialist);
        titleNonRussianSpecialist.setMinimumWidth(10); titleNonRussianSpecialist.setPreferredWidth(50);

        fileForeignResumeNonRussianSpecialist = addDProp("fileForeignResumeNonRussianSpecialist", "File Curriculum Vitae", PDFClass.instance, nonRussianSpecialist);
        loadFileForeignResumeNonRussianSpecialist = addLFAProp(baseGroup, "Load file Curriculum Vitae", fileForeignResumeNonRussianSpecialist);
        openFileForeignResumeNonRussianSpecialist = addOFAProp(baseGroup, "Open file Curriculum Vitae", fileForeignResumeNonRussianSpecialist);

        fileNativeResumeNonRussianSpecialist = addDProp("fileNativeResumeNonRussianSpecialist", "Файл резюме специалиста", PDFClass.instance, nonRussianSpecialist);
        loadFileNativeResumeNonRussianSpecialist = addLFAProp(baseGroup, "Загрузить файл резюме", fileNativeResumeNonRussianSpecialist);
        openFileNativeResumeNonRussianSpecialist = addOFAProp(baseGroup, "Открыть файл резюме", fileNativeResumeNonRussianSpecialist);

        filePassportNonRussianSpecialist = addDProp("filePassportNonRussianSpecialist", "Файл паспорта", PDFClass.instance, nonRussianSpecialist);
        loadFilePassportNonRussianSpecialist = addLFAProp(baseGroup, "Загрузить файл паспорта", filePassportNonRussianSpecialist);
        openFilePassportNonRussianSpecialist = addOFAProp(baseGroup, "Открыть файл паспорта", filePassportNonRussianSpecialist);

        fileStatementNonRussianSpecialist = addDProp("fileStatementNonRussianSpecialist", "Файл заявления", PDFClass.instance, nonRussianSpecialist);
        loadFileStatementNonRussianSpecialist = addLFAProp(baseGroup, "Загрузить файл заявления", fileStatementNonRussianSpecialist);
        openFileStatementNonRussianSpecialist = addOFAProp(baseGroup, "Открыть файл заявления", fileStatementNonRussianSpecialist);


        clusterVote = addDCProp(idGroup, "clusterVote", "Кластер (ИД)", true, clusterProject, true, projectVote, 1);
        clusterProjectVote = addJProp(idGroup, "clusterProjectVote", "Кластер проекта (ИД)", clusterProject, projectVote, 1);
        equalsClusterProjectVote = addJProp(baseGroup, true, "equalsClusterProjectVote", "Тек. кластер", baseLM.equals2, clusterVote, 1, clusterProjectVote, 1);
        nameNativeClusterVote = addJProp(baseGroup, "nameNativeClusterVote", "Кластер", nameNative, clusterVote, 1);

        clusterInExpertVote = addJProp("clusterInExpertVote", "Вкл", inClusterExpert, clusterVote, 2, 1);

        nameNativeClaimerVote = addJProp(baseGroup, "nameNativeClaimerVote", "Заявитель", nameNativeClaimerProject, projectVote, 1);
        nameNativeClaimerVote.setMinimumWidth(10); nameNativeClaimerVote.setPreferredWidth(50);
        nameForeignClaimerVote = addJProp(baseGroup, "nameForeignClaimerVote", "Заявитель (иностр.)", nameForeignClaimerProject, projectVote, 1);
        nameGenitiveClaimerVote = addJProp(baseGroup, "nameGenitiveClaimerVote", "Заявитель (кого)", nameGenitiveClaimerProject, projectVote, 1);
        nameDativusClaimerVote = addJProp(baseGroup, "nameDativusClaimerVote", "Заявитель (кому)", nameDativusClaimerProject, projectVote, 1);
        nameAblateClaimerVote = addJProp(baseGroup, "nameAblateClaimerVote", "Заявитель (кем)", nameAblateClaimerProject, projectVote, 1);

        documentTemplateDocumentTemplateDetail = addDProp(idGroup, "documentTemplateDocumentTemplateDetail", "Шаблон (ИД)", documentTemplate, documentTemplateDetail);

        projectDocument = addDProp(idGroup, "projectDocument", "Проект (ИД)", project, document);
        nameNativeProjectDocument = addJProp(baseGroup, "nameNativeProjectDocument", "Проект", nameNative, projectDocument, 1);

        quantityMinLanguageDocumentType = addDProp(baseGroup, "quantityMinLanguageDocumentType", "Мин. док.", IntegerClass.instance, language, documentType);
        quantityMaxLanguageDocumentType = addDProp(baseGroup, "quantityMaxLanguageDocumentType", "Макс. док.", IntegerClass.instance, language, documentType);
        LP singleLanguageDocumentType = addJProp("Один док.", baseLM.equals2, quantityMaxLanguageDocumentType, 1, 2, addCProp(IntegerClass.instance, 1));
        LP multipleLanguageDocumentType = addJProp(baseLM.andNot1, addCProp(LogicalClass.instance, true, language, documentType), 1, 2, singleLanguageDocumentType, 1, 2);
        translateLanguageDocumentType = addDProp(baseGroup, "translateLanguageDocumentType", "Перевод", StringClass.get(50), language, documentType);

        languageExpert = addDProp(idGroup, "languageExpert", "Язык (ИД)", language, expert);
        nameLanguageExpert = addJProp(baseGroup, "nameLanguageExpert", "Язык", baseLM.name, languageExpert, 1);

        languageDocument = addDProp(idGroup, "languageDocument", "Язык (ИД)", language, documentAbstract);
        nameLanguageDocument = addJProp(baseGroup, "nameLanguageDocument", "Язык", baseLM.name, languageDocument, 1);
        englishDocument = addJProp("englishDocument", "Иностр.", baseLM.equals2, languageDocument, 1, addCProp(language, "english"));

        defaultEnglishDocumentType = addDProp(baseGroup, "defaultEnglishDocumentType", "Англ.", LogicalClass.instance, documentType);

        typeDocument = addDProp(idGroup, "typeDocument", "Тип (ИД)", documentType, documentAbstract);
        nameTypeDocument = addJProp(baseGroup, "nameTypeDocument", "Тип", baseLM.name, typeDocument, 1);
        defaultEnglishDocument = addJProp("defaultEnglishDocument", "Англ.", defaultEnglishDocumentType, typeDocument, 1);

        localeLanguage = addDProp(baseGroup, "localeLanguage", "Locale", StringClass.get(5), language);
        authExpertSubjectLanguage = addDProp(baseGroup, "authExpertSubjectLanguage", "Заголовок аутентификации эксперта", StringClass.get(100), language);
        letterExpertSubjectLanguage = addDProp(baseGroup, "letterExpertSubjectLanguage", "Заголовок письма о заседании", StringClass.get(100), language);

        LP multipleDocument = addJProp(multipleLanguageDocumentType, languageDocument, 1, typeDocument, 1);
        postfixDocument = addJProp(baseLM.and1, addDProp("postfixDocument", "Доп. описание", StringClass.get(15), document), 1, multipleDocument, 1);
        hidePostfixDocument = addJProp(baseLM.and1, addCProp(StringClass.get(40), "Постфикс", document), 1, multipleDocument, 1);

        LP translateNameDocument = addJProp("Перевод", translateLanguageDocumentType, languageDocument, 1, typeDocument, 1);
        nameDocument = addJProp("nameDocument", "Заголовок", baseLM.string2, translateNameDocument, 1, addSUProp(Union.OVERRIDE, addCProp(StringClass.get(15), "", document), postfixDocument), 1);

        quantityProjectLanguageDocumentType = addSGProp("projectLanguageDocumentType", true, "Кол-во док.", addCProp(IntegerClass.instance, 1, document), projectDocument, 1, languageDocument, 1, typeDocument, 1); // сколько экспертов высказалось
        LP notEnoughProjectLanguageDocumentType = addSUProp(Union.OVERRIDE, addJProp(baseLM.greater2, quantityProjectLanguageDocumentType, 1, 2, 3, quantityMaxLanguageDocumentType, 2, 3),
                addJProp(baseLM.less2, addSUProp(Union.OVERRIDE, addCProp(IntegerClass.instance, 0, project, language, documentType), quantityProjectLanguageDocumentType), 1, 2, 3, quantityMinLanguageDocumentType, 2, 3));
        notEnoughProject = addMGProp(projectStatusGroup, "notEnoughProject", true, "Недостаточно док.", notEnoughProjectLanguageDocumentType, 1);

        autoGenerateProject = addDProp(projectStatusGroup, "autoGenerateProject", "Авт. зас.", LogicalClass.instance, project);

        fileDocument = addDProp(baseGroup, "fileDocument", "Файл", PDFClass.instance, document);
        loadFileDocument = addLFAProp(baseGroup, "Загрузить", fileDocument);
        openFileDocument = addOFAProp(baseGroup, "Открыть", fileDocument);

        inDefaultDocumentLanguage = addJProp("inDefaultDocumentLanguage", "Вкл. (по умолчанию)", and(false, false, true),
                englishDocument, 1, // если документ на английском
                defaultEnglishDocument, 1, // если для типа документа можно только на английском
                is(language), 2, // второй параметр - язык
                addJProp(addMGProp(object(document), projectDocument, 1, typeDocument, 1, postfixDocument, 1, languageDocument, 1), projectDocument, 1, typeDocument, 1, postfixDocument, 1, 2), 1, 2); // нету документа на русском
        inDefaultDocumentExpert = addJProp("inDefaultDocumentExpert", "Вкл.", inDefaultDocumentLanguage, 1, languageExpert, 2);

        inDocumentLanguage = addJProp("inDocumentLanguage", "Вкл.", baseLM.equals2, languageDocument, 1, 2);
        inDocumentExpert = addJProp("inDocumentExpert", "Вкл.", inDocumentLanguage, 1, languageExpert, 2);

        inExpertVote = addDProp(baseGroup, "inExpertVote", "Вкл", LogicalClass.instance, expert, vote); // !!! нужно отослать письмо с документами и т.д
        oldExpertVote = addDProp(baseGroup, "oldExpertVote", "Из предыдущего заседания", LogicalClass.instance, expert, vote); // !!! нужно отослать письмо с документами и т.д
        inNewExpertVote = addJProp(baseGroup, "inNewExpertVote", "Вкл (нов.)", baseLM.andNot1, inExpertVote, 1, 2, oldExpertVote, 1, 2);
        inOldExpertVote = addJProp(baseGroup, "inOldExpertVote", "Вкл (стар.)", baseLM.and1, inExpertVote, 1, 2, oldExpertVote, 1, 2);

        dateStartVote = addJProp(baseGroup, "dateStartVote", true, "Дата начала", baseLM.and1, baseLM.date, 1, is(vote), 1);
//        dateEndVote = addJProp(baseGroup, "dateEndVote", "Дата окончания", addDate2, dateStartVote, 1, requiredPeriod);
        aggrDateEndVote = addJProp(baseGroup, "aggrDateEndVote", "Дата окончания (агр.)", baseLM.addDate2, dateStartVote, 1, requiredPeriod);
        dateEndVote = addDProp(baseGroup, "dateEndVote", "Дата окончания", DateClass.instance, vote);
        dateEndVote.setDerivedForcedChange(true, aggrDateEndVote, 1, dateStartVote, 1);

        openedVote = addJProp(baseGroup, "openedVote", "Открыто", baseLM.groeq2, dateEndVote, 1, baseLM.currentDate);
        closedVote = addJProp(baseGroup, "closedVote", "Закрыто", baseLM.andNot1, is(vote), 1, openedVote, 1);

        voteInProgressProject = addAGProp(idGroup, "voteInProgressProject", true, "Тек. заседание (ИД)",
                openedVote, 1, projectVote, 1); // активно только одно заседание

        // результаты голосования
        voteResultExpertVote = addDProp(idGroup, "voteResultExpertVote", "Результат (ИД)", voteResult, expert, vote);
        voteResultNewExpertVote = addJProp(baseGroup, "voteResultNewExpertVote", "Результат (ИД) (новый)", baseLM.and1,
                voteResultExpertVote, 1, 2, inNewExpertVote, 1, 2);

        dateExpertVote = addDProp(voteResultCheckGroup, "dateExpertVote", "Дата рез.", DateClass.instance, expert, vote);

        doneExpertVote = addJProp(baseGroup, "doneExpertVote", "Проголосовал", baseLM.equals2,
                                  voteResultExpertVote, 1, 2, addCProp(voteResult, "voted"));
        doneNewExpertVote = addJProp(baseGroup, "doneNewExpertVote", "Проголосовал (новый)", baseLM.and1,
                                  doneExpertVote, 1, 2, inNewExpertVote, 1, 2);
        doneOldExpertVote = addJProp(baseGroup, "doneOldExpertVote", "Проголосовал (старый)", baseLM.and1,
                                  doneExpertVote, 1, 2, inOldExpertVote, 1, 2);

        refusedExpertVote = addJProp(baseGroup, "refusedExpertVote", "Проголосовал", baseLM.equals2,
                                  voteResultExpertVote, 1, 2, addCProp(voteResult, "refused"));

        connectedExpertVote = addJProp(baseGroup, "connectedExpertVote", "Аффилирован", baseLM.equals2,
                                  voteResultExpertVote, 1, 2, addCProp(voteResult, "connected"));

        nameVoteResultExpertVote = addJProp(voteResultCheckGroup, "nameVoteResultExpertVote", "Результат", baseLM.name, voteResultExpertVote, 1, 2);

        LP incrementVote = addJProp(baseLM.greater2, dateEndVote, 1, addCProp(DateClass.instance, new java.sql.Date(2011 - 1900, 4 - 1, 26)));
        inProjectExpert = addMGProp(baseGroup, "inProjectExpert", "Вкл. в заседания", inExpertVote, projectVote, 2, 1);
        voteProjectExpert = addAGProp(baseGroup, "voteProjectExpert", "Результ. заседание", addJProp(baseLM.and1, voteResultNewExpertVote, 1, 2, incrementVote, 2), 2, projectVote, 2);
        voteResultProjectExpert = addJProp(baseGroup, "voteResultProjectExpert", "Результ. заседания", voteResultExpertVote, 2, voteProjectExpert, 1, 2);
        doneProjectExpert = addJProp(baseGroup, "doneProjectExpert", "Проголосовал", baseLM.equals2, voteResultProjectExpert, 1, 2, addCProp(voteResult, "voted"));
        LP doneProject = addSGProp(baseGroup, "doneProject", "Проголосовало", addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), doneProjectExpert, 1, 2), 2); // сколько экспертов высказалось

        inClusterExpertVote = addDProp(voteResultCheckGroup, "inClusterExpertVote", "Соотв-ие кластеру", LogicalClass.instance, expert, vote);
        inClusterNewExpertVote = addJProp(baseGroup, "inClusterNewExpertVote", "Соотв-ие кластеру (новый)", baseLM.and1,
                inClusterExpertVote, 1, 2, inNewExpertVote, 1, 2);

        innovativeExpertVote = addDProp(voteResultCheckGroup, "innovativeExpertVote", "Инновац.", LogicalClass.instance, expert, vote);
        innovativeNewExpertVote = addJProp(baseGroup, "innovativeNewExpertVote", "Инновац. (новый)", baseLM.and1,
                innovativeExpertVote, 1, 2, inNewExpertVote, 1, 2);

        foreignExpertVote = addDProp(voteResultCheckGroup, "foreignExpertVote", "Иностр. специалист", LogicalClass.instance, expert, vote);
        foreignNewExpertVote = addJProp(baseGroup, "foreignNewExpertVote", "Иностр. специалист (новый)", baseLM.and1,
                foreignExpertVote, 1, 2, inNewExpertVote, 1, 2);

        innovativeCommentExpertVote = addDProp(voteResultCommentGroup, "innovativeCommentExpertVote", "Инновационность (комм.)", TextClass.instance, expert, vote);
        competentExpertVote = addDProp(voteResultCheckGroup, "competentExpertVote", "Компет.", IntegerClass.instance, expert, vote);
        completeExpertVote = addDProp(voteResultCheckGroup, "completeExpertVote", "Полнота информ.", IntegerClass.instance, expert, vote);
        completeCommentExpertVote = addDProp(voteResultCommentGroup, "completeCommentExpertVote", "Полнота информации (комм.)", TextClass.instance, expert, vote);

        followed(doneExpertVote, inClusterExpertVote, innovativeExpertVote, foreignExpertVote, innovativeCommentExpertVote, competentExpertVote, completeExpertVote, completeCommentExpertVote);
        followed(voteResultExpertVote, dateExpertVote);

        quantityInVote = addSGProp(voteResultGroup, "quantityInVote", true, "Учавствовало",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), inExpertVote, 1, 2), 2); // сколько экспертов учавстовало

        quantityRepliedVote = addSGProp(voteResultGroup, "quantityRepliedVote", true, "Ответило",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), voteResultExpertVote, 1, 2), 2); // сколько экспертов высказалось

        quantityDoneVote = addSGProp(voteResultGroup, "quantityDoneVote", true, "Проголосовало",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), doneExpertVote, 1, 2), 2); // сколько экспертов высказалось

        quantityDoneNewVote = addSGProp(voteResultGroup, "quantityDoneNewVote", true, "Проголосовало (нов.)",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), doneNewExpertVote, 1, 2), 2); // сколько новых экспертов высказалось

        quantityDoneOldVote = addSGProp(voteResultGroup, "quantityDoneOldVote", true, "Проголосовало (пред.)",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), doneOldExpertVote, 1, 2), 2); // сколько старых экспертов высказалось

        quantityRefusedVote = addSGProp(voteResultGroup, "quantityRefusedVote", true, "Отказалось",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), refusedExpertVote, 1, 2), 2); // сколько экспертов высказалось

        quantityConnectedVote = addSGProp(voteResultGroup, "quantityConnectedVote", true, "Аффилировано",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), connectedExpertVote, 1, 2), 2); // сколько экспертов высказалось

        quantityInClusterVote = addSGProp(voteResultGroup, "quantityInClusterVote", true, "Соотв-ие кластеру (голоса)",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), inClusterExpertVote, 1, 2), 2); // сколько экспертов высказалось

        quantityInnovativeVote = addSGProp(voteResultGroup, "quantityInnovativeVote", true, "Инновац. (голоса)",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), innovativeExpertVote, 1, 2), 2); // сколько экспертов высказалось

        quantityForeignVote = addSGProp(voteResultGroup, "quantityForeignVote", true, "Иностр. специалист (голоса)",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), foreignExpertVote, 1, 2), 2); // сколько экспертов высказалось

        acceptedInClusterVote = addJProp(voteResultGroup, "acceptedInClusterVote", "Соотв-ие кластеру", baseLM.greater2,
                                          addJProp(baseLM.multiplyIntegerBy2, quantityInClusterVote, 1), 1,
                                          quantityDoneVote, 1);

        acceptedInnovativeVote = addJProp(voteResultGroup, "acceptedInnovativeVote", "Инновац.", baseLM.greater2,
                                           addJProp(baseLM.multiplyIntegerBy2, quantityInnovativeVote, 1), 1,
                                           quantityDoneVote, 1);

        acceptedForeignVote = addJProp(voteResultGroup, "acceptedForeignVote", "Иностр. специалист", baseLM.greater2,
                                        addJProp(baseLM.multiplyIntegerBy2, quantityForeignVote, 1), 1,
                                        quantityDoneVote, 1);

        acceptedVote = addJProp(voteResultGroup, "acceptedVote", true, "Положительно", and(false, false),
                                acceptedInClusterVote, 1, acceptedInnovativeVote, 1, acceptedForeignVote, 1);

        succeededVote = addJProp(voteResultGroup, "succeededVote", true, "Состоялось", baseLM.groeq2, quantityDoneVote, 1, limitExperts); // достаточно экспертов
        succeededClusterVote = addJProp("succeededClusterVote", "Состоялось в тек. кластере", baseLM.and1, succeededVote, 1, equalsClusterProjectVote, 1);

        LP betweenExpertVoteDateFromDateTo = addJProp(baseLM.betweenDates, dateExpertVote, 1, 2, 3, 4);
        doneExpertVoteDateFromDateTo = addJProp(baseLM.and1, doneNewExpertVote, 1, 2, betweenExpertVoteDateFromDateTo, 1, 2, 3, 4);
        quantityDoneExpertDateFromDateTo = addSGProp("quantityDoneExpertDateFromDateTo", "Кол-во голосов.",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), doneExpertVoteDateFromDateTo, 1, 2, 3, 4), 1, 3, 4); // в скольки заседаниях поучавствовал
        inExpertVoteDateFromDateTo = addJProp(baseLM.and1, inNewExpertVote, 1, 2, betweenExpertVoteDateFromDateTo, 1, 2, 3, 4);
        quantityInExpertDateFromDateTo = addSGProp("quantityInExpertDateFromDateTo", "Кол-во участ.",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), inExpertVoteDateFromDateTo, 1, 2, 3, 4), 1, 3, 4); // в скольки заседаниях поучавствовал

        voteSucceededProject = addAGProp(idGroup, "voteSucceededProject", true, "Успешное заседание (ИД)", succeededClusterVote, 1, projectVote, 1);

        noCurrentVoteProject = addJProp(projectStatusGroup, "noCurrentVoteProject", "Нет текущих заседаний", baseLM.andNot1, is(project), 1, voteInProgressProject, 1); // нету текущих заседаний

        voteValuedProject = addJProp(idGroup, "voteValuedProject", true, "Оцененнное заседание (ИД)", baseLM.and1, voteSucceededProject, 1, noCurrentVoteProject, 1); // нет открытого заседания и есть состояшееся заседания

        acceptedProject = addJProp(projectStatusGroup, "acceptedProject", "Оценен положительно", acceptedVote, voteValuedProject, 1);

        needExtraVoteProject = addJProp("needExtraVoteProject", true, "Треб. заседание", and(true, true, true),
                                        is(project), 1,
                                        notEnoughProject, 1,
                                        voteInProgressProject, 1,
                                        voteSucceededProject, 1); // есть открытое заседания и есть состояшееся заседания !!! нужно создать новое заседание

        addConstraint(addJProp("Эксперт не соответствует необходимому кластеру", baseLM.andNot1,
                inExpertVote, 1, 2, clusterInExpertVote, 1, 2), false);
//        addConstraint(addJProp("Эксперт не соответствует необходимому кластеру", baseLM.diff2,
//                clusterExpert, 1, addJProp(baseLM.and1, clusterVote, 2, inExpertVote, 1, 2), 1, 2), false);

//        addConstraint(addJProp("Количество экспертов не соответствует требуемому", baseLM.andNot1, is(vote), 1, addJProp(baseLM.equals2, requiredQuantity,
//                addSGProp(addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), inExpertVote, 2, 1), 1), 1), 1), false);

        generateDocumentsProjectDocumentType = addAProp(actionGroup, new GenerateDocumentsActionProperty());
        includeDocumentsProjectDocumentType = addAProp(actionGroup, new IncludeDocumentsActionProperty());
        importProjectsAction = addAProp(actionGroup, new ImportProjectsActionProperty(this));

        generateVoteProject = addAProp(actionGroup, new GenerateVoteActionProperty());
        copyResultsVote = addAProp(actionGroup, new CopyResultsActionProperty());
        hideGenerateVoteProject = addHideCaptionProp(privateGroup, "Сгенерировать заседание", generateVoteProject, needExtraVoteProject);
//        generateVoteProject.setDerivedForcedChange(addCProp(ActionClass.instance, true), needExtraVoteProject, 1, autoGenerateProject, 1);

        baseLM.generateLoginPassword.setDerivedForcedChange(addCProp(ActionClass.instance, true), is(expert), 1);

        expertLogin = addAGProp(baseGroup, "expertLogin", "Эксперт (ИД)", baseLM.userLogin);
        disableExpert = addDProp(baseGroup, "disableExpert", "Не акт.", LogicalClass.instance, expert);

//        LP userRole = addCUProp("userRole", true, "Роль пользователя", baseLM.customUserSIDMainRole);
        LP userRole = addSUProp("userRole", true, "Роль пользователя", Union.OVERRIDE, baseLM.customUserSIDMainRole, addCProp(StringClass.get(30), "expert", expert));

        statusProject = addCaseUProp(idGroup, "statusProject", true, "Статус (ИД)",
                voteValuedProject, 1, addIfElseUProp(addCProp(projectStatus, "accepted", project), addCProp(projectStatus, "rejected", project), acceptedProject, 1), 1,
                voteSucceededProject, 1, addCProp(projectStatus, "succeeded", project), 1,
                voteInProgressProject, 1, addCProp(projectStatus, "inProgress", project), 1,
                needExtraVoteProject, 1, addCProp(projectStatus, "needExtraVote", project), 1,
                notEnoughProject, 1, addCProp(projectStatus, "needDocuments", project), 1);


/*        statusProject = addIfElseUProp(idGroup, "statusProject", true, "Статус (ИД)",
                                  addCProp(projectStatus, "accepted", project),
                                  addIfElseUProp(addCProp(projectStatus, "rejected", project),
                                                 addIfElseUProp(addCProp(projectStatus, "succeeded", project),
                                                                addIfElseUProp(addCProp(projectStatus, "inProgress", project),
                                                                               addIfElseUProp(addCProp(projectStatus, "needExtraVote", project),
                                                                                           addIfElseUProp(addCProp(projectStatus, "needDocuments", project),
                                                                                                 addCProp(projectStatus, "unknown", project),
                                                                                                 notEnoughProject, 1),
                                                                                           needExtraVoteProject, 1),
                                                                               voteInProgressProject, 1),
                                                                voteSucceededProject, 1),
                                                 voteRejectedProject, 1),
                                  acceptedProject, 1);*/
        nameStatusProject = addJProp(projectInformationGroup, "nameStatusProject", "Статус", baseLM.name, statusProject, 1);

        dateProject = addJProp("dateProject", "Дата проекта", baseLM.and1, baseLM.date, 1, is(project), 1);
        statusProjectVote = addJProp(idGroup, "statusProjectVote", "Статус проекта (ИД)", statusProject, projectVote, 1);
        nameStatusProjectVote = addJProp(baseGroup, "nameStatusProjectVote", "Статус проекта", baseLM.name, statusProjectVote, 1);

        projectSucceededClaimer = addAGProp(idGroup, "projectSucceededClaimer", true, "Успешный проект (ИД)", acceptedProject, 1, claimerProject, 1);


        // статистика по экспертам
        quantityTotalExpert = addSGProp(expertResultGroup, "quantityTotalExpert", "Всего заседаний",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), inNewExpertVote, 1, 2), 1);

        quantityDoneExpert = addSGProp(expertResultGroup, "quantityDoneExpert", "Проголосовал",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), doneNewExpertVote, 1, 2), 1);
        percentDoneExpert = addJProp(expertResultGroup, "percentDoneExpert", "Проголосовал (%)", percent, quantityDoneExpert, 1, quantityTotalExpert, 1);

        LP quantityInClusterExpert = addSGProp("quantityInClusterExpert", "Соотв-ие кластеру (голоса)",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), inClusterNewExpertVote, 1, 2), 1);
        percentInClusterExpert = addJProp(expertResultGroup, "percentInClusterExpert", "Соотв-ие кластеру (%)", percent, quantityInClusterExpert, 1, quantityDoneExpert, 1);

        LP quantityInnovativeExpert = addSGProp("quantityInnovativeExpert", "Инновац. (голоса)",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), innovativeNewExpertVote, 1, 2), 1);
        percentInnovativeExpert = addJProp(expertResultGroup, "percentInnovativeExpert", "Инновац. (%)", percent, quantityInnovativeExpert, 1, quantityDoneExpert, 1);

        LP quantityForeignExpert = addSGProp("quantityForeignExpert", "Иностр. специалист (голоса)",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), foreignNewExpertVote, 1, 2), 1);
        percentForeignExpert = addJProp(expertResultGroup, "percentForeignExpert", "Иностр. специалист (%)", percent, quantityForeignExpert, 1, quantityDoneExpert, 1);

        prevDateStartVote = addOProp("prevDateStartVote", "Пред. засед. (старт)", OrderType.PREVIOUS, dateStartVote, true, true, 1, projectVote, 1, baseLM.date, 1);
        prevDateVote = addOProp("prevDateVote", "Пред. засед. (окончание)", OrderType.PREVIOUS, dateEndVote, true, true, 1, projectVote, 1, baseLM.date, 1);
        numberNewExpertVote = addOProp("numberNewExpertVote", "Номер (нов.)", OrderType.SUM, addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), inNewExpertVote, 1, 2), true, true, 1, 2, 1);
        numberOldExpertVote = addOProp("numberOldExpertVote", "Номер (стар.)", OrderType.SUM, addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), inOldExpertVote, 1, 2), true, true, 1, 2, 1);

        emailDocuments = addDProp(baseGroup, "emailDocuments", "E-mail для документов", StringClass.get(50));

        emailLetterExpertVoteEA = addEAProp(expert, vote);
        addEARecepient(emailLetterExpertVoteEA, baseLM.email, 1);

        emailLetterExpertVote = addJProp(baseGroup, true, "emailLetterExpertVote", "Письмо о заседании (e-mail)",
                emailLetterExpertVoteEA, 1, 2, addJProp(letterExpertSubjectLanguage, languageExpert, 1), 1);
        emailLetterExpertVote.setImage("email.png");
        emailLetterExpertVote.property.askConfirm = true;
        emailLetterExpertVote.setDerivedForcedChange(addCProp(ActionClass.instance, true), inNewExpertVote, 1, 2);

        allowedEmailLetterExpertVote = addJProp(baseGroup, "Письмо о заседании (e-mail)", "Письмо о заседании", baseLM.andNot1, emailLetterExpertVote, 1, 2, voteResultExpertVote, 1, 2);
        allowedEmailLetterExpertVote.property.askConfirm = true;

        emailClaimerFromAddress = addDProp("emailClaimerFromAddress", "Адрес отправителя (для заявителей)", StringClass.get(50));
        emailClaimerVoteEA = addEAProp(actionGroup, "emailClaimerVoteEA", "Письмо заявителю", "Уведомление", emailClaimerFromAddress, vote);

        claimerEmailVote = addJProp("claimerEmailVote", "E-mail (заявителя)", baseLM.email, claimerVote, 1);
        addEARecepient(emailClaimerVoteEA, claimerEmailVote, 1);

        emailClaimerVoteEA.setDerivedForcedChange(addCProp(ActionClass.instance, true), openedVote, 1);

        emailStartVoteEA = addEAProp(vote);
        addEARecepient(emailStartVoteEA, emailDocuments);

        emailClaimerNameVote = addJProp(addSFProp("(CAST(prm1 as text))||(CAST(prm2 as text))", StringClass.get(2000), 2), object(StringClass.get(2000)), 2, nameNativeClaimerVote, 1);

        emailStartHeaderVote = addJProp("emailStartHeaderVote", "Заголовок созыва заседания", emailClaimerNameVote, 1, addCProp(StringClass.get(2000), "Созыв заседания - "));
        emailStartVote = addJProp(baseGroup, true, "emailStartVote", "Созыв заседания (e-mail)", emailStartVoteEA, 1, emailStartHeaderVote, 1);
        emailStartVote.property.askConfirm = true;
//        emailStartVote.setDerivedForcedChange(addCProp(ActionClass.instance, true), openedVote, 1);

        emailProtocolVoteEA = addEAProp(vote);
        addEARecepient(emailProtocolVoteEA, emailDocuments);

        emailProtocolHeaderVote = addJProp("emailProtocolHeaderVote", "Заголовок протокола заседания", emailClaimerNameVote, 1, addCProp(StringClass.get(2000), "Протокол заседания - "));
        emailProtocolVote = addJProp(baseGroup, true, "emailProtocolVote", "Протокол заседания (e-mail)", emailProtocolVoteEA, 1, emailProtocolHeaderVote, 1);
        emailProtocolVote.property.askConfirm = true;
//        emailProtocolVote.setDerivedForcedChange(addCProp(ActionClass.instance, true), closedVote, 1);

        emailClosedVoteEA = addEAProp(vote);
        addEARecepient(emailClosedVoteEA, emailDocuments);

        emailClosedHeaderVote = addJProp(emailClaimerNameVote, 1, addCProp(StringClass.get(2000), "Результаты заседания - "));
        emailClosedVote = addJProp(baseGroup, true, "emailClosedVote", "Результаты заседания (e-mail)", emailClosedVoteEA, 1, emailClosedHeaderVote, 1);
        emailClosedVote.setImage("email.png");
        emailClosedVote.property.askConfirm = true;
        emailClosedVote.setDerivedForcedChange(addCProp(ActionClass.instance, true), closedVote, 1);

        isForeignExpert = addJProp("isForeignExpert", "Иностр.", baseLM.equals2, languageExpert, 1, addCProp(language, "english"));
        localeExpert = addJProp("localeExpert", "Locale", localeLanguage, languageExpert, 1);

        emailAuthExpertEA = addEAProp(expert);
        addEARecepient(emailAuthExpertEA, baseLM.email, 1);

        emailAuthExpert = addJProp(baseGroup, true, "emailAuthExpert", "Аутентификация эксперта (e-mail)",
                emailAuthExpertEA, 1, addJProp(authExpertSubjectLanguage, languageExpert, 1), 1);
        emailAuthExpert.setImage("email.png");
        emailAuthExpert.property.askConfirm = true;
//        emailAuthExpert.setDerivedChange(addCProp(ActionClass.instance, true), userLogin, 1, userPassword, 1);

        emailClaimerAcceptedHeaderVote = addJProp(emailClaimerNameVote, 1, addCProp(StringClass.get(2000), "Решение о соответствии - "));
        emailClaimerRejectedHeaderVote = addJProp(emailClaimerNameVote, 1, addCProp(StringClass.get(2000), "Решение о несоответствии - "));

        emailAcceptedProjectEA = addEAProp(project);
        addEARecepient(emailAcceptedProjectEA, emailDocuments);
        emailClaimerAcceptedHeaderProject = addJProp(addSFProp("(CAST(prm1 as text))||(CAST(prm2 as text))", StringClass.get(2000), 2), addCProp(StringClass.get(2000), "Решение о присвоении статуса участника - "), nameNativeClaimerProject, 1);
        emailAcceptedProject = addJProp(baseGroup, true, "emailAcceptedProject", "Решение о присвоении статуса участника (e-mail)", emailAcceptedProjectEA, 1, emailClaimerAcceptedHeaderProject, 1);
        emailAcceptedProject.setImage("email.png");
        emailAcceptedProject.property.askConfirm = true;
        emailAcceptedProject.setDerivedForcedChange(addCProp(ActionClass.instance, true), acceptedProject, 1);

        emailToExpert = addJProp("emailToExpert", "Эксперт по e-mail", addJProp(baseLM.and1, 1, is(expert), 1), baseLM.emailToObject, 1);
    }

    @Override
    public void initIndexes() {
        addIndex(inExpertVote);
        addIndex(projectVote);
        addIndex(projectDocument);
    }


    FormEntity languageDocumentTypeForm;
    FormEntity globalForm;
    public ProjectFullFormEntity projectFullNative;
    public ProjectFullFormEntity projectFullForeign;

    @Override
    public void initNavigators() throws JRException, FileNotFoundException {

        ToolBarNavigatorWindow mainToolbar = new ToolBarNavigatorWindow(JToolBar.VERTICAL, "mainToolbar", "Навигатор", 0, 0, 7, 100);
        mainToolbar.titleShown = false;
        mainToolbar.verticalTextPosition = SwingConstants.BOTTOM;
        mainToolbar.horizontalTextPosition = SwingConstants.CENTER;
        mainToolbar.horizontalAlignment = SwingConstants.CENTER;

        ToolBarNavigatorWindow leftToolbar = new ToolBarNavigatorWindow(JToolBar.VERTICAL, "leftToolbar", "Список", 7, 0, 13, 60);

        baseLM.baseElement.window = mainToolbar;
        baseLM.adminElement.window = leftToolbar;

        TreeNavigatorWindow objectsWindow = new TreeNavigatorWindow("objectsWindow", "Объекты", 7, 30, 13, 40);
        objectsWindow.drawRoot = true;
        baseLM.objectElement.window = objectsWindow;

        projectFullNative = addFormEntity(new ProjectFullFormEntity(baseLM.objectElement, "projectFullNative", "Резюме проекта для эксперта", false));
        project.setEditForm(projectFullNative);
        projectFullForeign = addFormEntity(new ProjectFullFormEntity(baseLM.objectElement, "projectFullForeign", "Resume project for expert", true));

        claimer.setEditForm(addFormEntity(new ClaimerFullFormEntity(baseLM.objectElement, "claimerFull")));

        NavigatorElement print = new NavigatorElement(baseLM.baseElement, "print", "Печатные формы");
        print.window = leftToolbar;

        addFormEntity(new VoteStartFormEntity(print, "voteStart"));
        addFormEntity(new ExpertLetterFormEntity(print, "expertLetter"));
        addFormEntity(new VoteProtocolFormEntity(print, "voteProtocol"));
        addFormEntity(new ExpertProtocolFormEntity(print, "expertProtocol"));
        addFormEntity(new ExpertAuthFormEntity(print, "expertAuth"));
        addFormEntity(new ClaimerAcceptedFormEntity(print, "claimerAccepted"));
        addFormEntity(new ClaimerRejectedFormEntity(print, "claimerRejected"));
        addFormEntity(new ClaimerStatusFormEntity(print, "claimerStatus"));
        addFormEntity(new VoteClaimerFormEntity(print, "voteClaimer", "Уведомление о рассмотрении"));

        addFormEntity(new ProjectFormEntity(baseLM.baseElement, "project"));
        addFormEntity(new ClaimerFormEntity(baseLM.baseElement, "claimer"));
        addFormEntity(new VoteFormEntity(baseLM.baseElement, "vote", false));
        addFormEntity(new ExpertFormEntity(baseLM.baseElement, "expert"));
        addFormEntity(new VoteExpertFormEntity(baseLM.baseElement, "voteExpert"));
        addFormEntity(new VoteFormEntity(baseLM.baseElement, "voterestricted", true));

        baseLM.baseElement.add(print);

        NavigatorElement options = new NavigatorElement(baseLM.baseElement, "options", "Настройки");
        options.window = leftToolbar;

        languageDocumentTypeForm = addFormEntity(new LanguageDocumentTypeFormEntity(options, "languageDocumentType"));
        addFormEntity(new DocumentTemplateFormEntity(options, "documentTemplate"));
        globalForm = addFormEntity(new GlobalFormEntity(options, "global"));

        baseLM.baseElement.add(baseLM.adminElement); // перемещаем adminElement в конец
    }

    @Override
    public String getNamePrefix() {
        return null;
    }

    private class ProjectFullFormEntity extends ClassFormEntity<SkolkovoBusinessLogics> {

        private boolean foreign;

        private ObjectEntity objProject;
        private ObjectEntity objPatent;
        private ObjectEntity objAcademic;
        private ObjectEntity objNonRussianSpecialist;

        private ProjectFullFormEntity(NavigatorElement parent, String sID, String caption, boolean foreign) {
            super(parent, sID, caption);

            this.foreign = foreign;

            objProject = addSingleGroupObject(1, "project", project, "Описание проекта", projectInformationGroup, innovationGroup, executiveSummaryGroup, sourcesFundingGroup, equipmentGroup, projectDocumentsGroup, projectStatusGroup);

            getPropertyDraw(nameReturnInvestorProject).propertyCaption = addPropertyObject(hideNameReturnInvestorProject, objProject);
            getPropertyDraw(amountReturnFundsProject).propertyCaption = addPropertyObject(hideAmountReturnFundsProject, objProject);
            getPropertyDraw(nameNonReturnInvestorProject).propertyCaption = addPropertyObject(hideNameNonReturnInvestorProject, objProject);
            getPropertyDraw(amountNonReturnFundsProject).propertyCaption = addPropertyObject(hideAmountNonReturnFundsProject, objProject);

            getPropertyDraw(isCapitalInvestmentProject).propertyCaption = addPropertyObject(hideIsCapitalInvestmentProject, objProject);
            getPropertyDraw(isPropertyInvestmentProject).propertyCaption = addPropertyObject(hideIsPropertyInvestmentProject, objProject);
            getPropertyDraw(isGrantsProject).propertyCaption = addPropertyObject(hideIsGrantsProject, objProject);
            getPropertyDraw(isOtherNonReturnInvestmentsProject).propertyCaption = addPropertyObject(hideIsOtherNonReturnInvestmentsProject, objProject);

            getPropertyDraw(commentOtherNonReturnInvestmentsProject).propertyCaption = addPropertyObject(hideCommentOtherNonReturnInvestmentsProject, objProject);
            getPropertyDraw(amountOwnFundsProject).propertyCaption = addPropertyObject(hideAmountOwnFundsProject, objProject);
            getPropertyDraw(amountFundsProject).propertyCaption = addPropertyObject(hideAmountFundsProject, objProject);
            getPropertyDraw(commentOtherSoursesProject).propertyCaption = addPropertyObject(hideCommentOtherSoursesProject, objProject);

            getPropertyDraw(descriptionTransferEquipmentProject).propertyCaption = addPropertyObject(hideDescriptionTransferEquipmentProject, objProject);
            getPropertyDraw(ownerEquipmentProject).propertyCaption = addPropertyObject(hideOwnerEquipmentProject, objProject);
            getPropertyDraw(specificationEquipmentProject).propertyCaption = addPropertyObject(hideSpecificationEquipmentProject, objProject);
            getPropertyDraw(descriptionEquipmentProject).propertyCaption = addPropertyObject(hideDescriptionEquipmentProject, objProject);
            getPropertyDraw(commentEquipmentProject).propertyCaption = addPropertyObject(hideCommentEquipmentProject, objProject);

            objProject.groupTo.setSingleClassView(ClassViewType.PANEL);

            objPatent = addSingleGroupObject(2, "patent", patent, "Патент", baseGroup);
            addObjectActions(this, objPatent);

            getPropertyDraw(ownerPatent).propertyCaption = addPropertyObject(hideOwnerPatent, objPatent);
            getPropertyDraw(nameOwnerTypePatent).propertyCaption = addPropertyObject(hideNameOwnerTypePatent, objPatent);
            getPropertyDraw(loadFileIntentionOwnerPatent).propertyCaption = addPropertyObject(hideLoadFileIntentionOwnerPatent, objPatent);
            getPropertyDraw(openFileIntentionOwnerPatent).propertyCaption = addPropertyObject(hideOpenFileIntentionOwnerPatent, objPatent);

            getPropertyDraw(valuatorPatent).propertyCaption = addPropertyObject(hideValuatorPatent, objPatent);
            getPropertyDraw(loadFileActValuationPatent).propertyCaption = addPropertyObject(hideLoadFileActValuationPatent, objPatent);
            getPropertyDraw(openFileActValuationPatent).propertyCaption = addPropertyObject(hideOpenFileActValuationPatent, objPatent);

            objAcademic = addSingleGroupObject(3, "academic", academic, "Учёный", baseGroup);
            addObjectActions(this, objAcademic);

            objNonRussianSpecialist = addSingleGroupObject(4, "nonRussianSpecialist", nonRussianSpecialist, "Иностранный специалист", baseGroup);
            addObjectActions(this, objNonRussianSpecialist);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectPatent, objPatent), Compare.EQUALS, objProject));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectAcademic, objAcademic), Compare.EQUALS, objProject));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectNonRussianSpecialist, objNonRussianSpecialist), Compare.EQUALS, objProject));

            addProject = addMFAProp(actionGroup, "Добавить", this, new ObjectEntity[]{}, true, addPropertyObject(getAddObjectAction(project)));
            editProject = addMFAProp(actionGroup, "Редактировать", this, new ObjectEntity[]{objProject}).setImage("edit.png");
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.getGroupPropertyContainer(objProject.groupTo, innovationGroup).constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            design.getGroupPropertyContainer(objProject.groupTo, executiveSummaryGroup).constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            design.getGroupPropertyContainer(objProject.groupTo, sourcesFundingGroup).constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            design.getGroupPropertyContainer(objProject.groupTo, equipmentGroup).constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            design.getGroupPropertyContainer(objProject.groupTo, projectDocumentsGroup).constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            design.getGroupPropertyContainer(objProject.groupTo, projectStatusGroup).constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;

            design.addIntersection(design.getGroupPropertyContainer(objProject.groupTo, innovationGroup),
                                   design.getGroupPropertyContainer(objProject.groupTo, sourcesFundingGroup), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupPropertyContainer(objProject.groupTo, executiveSummaryGroup),
                                   design.getGroupPropertyContainer(objProject.groupTo, sourcesFundingGroup), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupPropertyContainer(objProject.groupTo, projectDocumentsGroup),
                                   design.getGroupPropertyContainer(objProject.groupTo, projectStatusGroup), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            ContainerView specContainer = design.createContainer();
            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objProject.groupTo));
            specContainer.add(design.getGroupObjectContainer(objProject.groupTo));
            specContainer.add(design.getGroupObjectContainer(objPatent.groupTo));
            specContainer.add(design.getGroupObjectContainer(objAcademic.groupTo));
            specContainer.add(design.getGroupObjectContainer(objNonRussianSpecialist.groupTo));
            specContainer.tabbedPane = true;

            design.getMainContainer().addBefore(design.getGroupPropertyContainer(objProject.groupTo, projectInformationGroup), specContainer);

            design.setShowTableFirstLogical(true);

            return design;
        }

        @Override
        public ObjectEntity getObject() {
            return objProject;
        }
    }

    private class ProjectFormEntity extends FormEntity<SkolkovoBusinessLogics> {
        private ObjectEntity objProject;
        private ObjectEntity objVote;
        private ObjectEntity objDocument;
        private ObjectEntity objExpert;
        private ObjectEntity objDocumentTemplate;
        private RegularFilterGroupEntity projectFilterGroup;

        private ProjectFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Реестр проектов");


            objProject = addSingleGroupObject(project, baseLM.date, nameNative, nameForeign,  nameNativeClusterProject, nameNativeJoinClaimerProject, nameStatusProject, autoGenerateProject, generateVoteProject, editProject);
            addObjectActions(this, objProject);

//            addPropertyDraw(addProject).toDraw = objProject.groupTo;
//            getPropertyDraw(addProject).forceViewType = ClassViewType.PANEL;

            getPropertyDraw(generateVoteProject).forceViewType = ClassViewType.PANEL;
            getPropertyDraw(generateVoteProject).propertyCaption = addPropertyObject(hideGenerateVoteProject, objProject);

            objVote = addSingleGroupObject(vote, dateStartVote, dateEndVote, nameNativeClusterVote, equalsClusterProjectVote, openedVote, succeededVote, acceptedVote, quantityDoneVote, quantityInClusterVote, quantityInnovativeVote, quantityForeignVote, copyResultsVote, emailClaimerVoteEA, baseLM.delete);
            objVote.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.PANEL, ClassViewType.HIDE));

            getPropertyDraw(copyResultsVote).forceViewType = ClassViewType.PANEL;

            objDocumentTemplate = addSingleGroupObject(documentTemplate, "Шаблон документов", baseLM.name);
            objDocumentTemplate.groupTo.setSingleClassView(ClassViewType.PANEL);
            setReadOnly(objDocumentTemplate, true);
            addPropertyDraw(generateDocumentsProjectDocumentType, objProject, objDocumentTemplate);
            addPropertyDraw(includeDocumentsProjectDocumentType, objProject, objDocumentTemplate);
            addPropertyDraw(importProjectsAction, objProject, objDocumentTemplate);

            objDocument = addSingleGroupObject(document, nameTypeDocument, nameLanguageDocument, postfixDocument, loadFileDocument, openFileDocument);
            addObjectActions(this, objDocument);
            getPropertyDraw(postfixDocument).forceViewType = ClassViewType.PANEL;
            getPropertyDraw(postfixDocument).propertyCaption = addPropertyObject(hidePostfixDocument, objDocument);

            objExpert = addSingleGroupObject(expert);
            addPropertyDraw(objExpert, objVote, inExpertVote, oldExpertVote);
            addPropertyDraw(objExpert, baseLM.name, documentNameExpert, baseLM.email);
            addPropertyDraw(voteResultGroup, true, objExpert, objVote);

            setForceViewType(voteResultCommentGroup, ClassViewType.PANEL);


            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectVote, objVote), Compare.EQUALS, objProject));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectDocument, objDocument), Compare.EQUALS, objProject));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(clusterInExpertVote, objExpert, objVote)));

            RegularFilterGroupEntity expertFilterGroup = new RegularFilterGroupEntity(genID());
            expertFilterGroup.addFilter(new RegularFilterEntity(genID(),
                                                                new NotNullFilterEntity(addPropertyObject(inExpertVote, objExpert, objVote)),
                                                                "В заседании",
                                                                KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)), true);
            addRegularFilterGroup(expertFilterGroup);

            projectFilterGroup = new RegularFilterGroupEntity(genID());
            projectFilterGroup.addFilter(new RegularFilterEntity(genID(),
                                                                 new NotNullFilterEntity(addPropertyObject(voteValuedProject, objProject)),
                                                                 "Оценен",
                                                                 KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            addRegularFilterGroup(projectFilterGroup);

            addHintsNoUpdate(statusProject);
            setPageSize(0);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

//            design.get(getPropertyDraw(addProject)).drawToToolbar = true;

            ContainerView specContainer = design.createContainer();
            specContainer.tabbedPane = true;

            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objProject.groupTo));

            ContainerView expertContainer = design.createContainer("Экспертиза по существу");
            expertContainer.add(design.getGroupObjectContainer(objVote.groupTo));
            expertContainer.add(design.getGroupObjectContainer(objExpert.groupTo));

            ContainerView docContainer = design.createContainer("Документы");
            docContainer.add(design.getGroupObjectContainer(objDocumentTemplate.groupTo));
            docContainer.add(design.getGroupObjectContainer(objDocument.groupTo));

            specContainer.add(docContainer);
            specContainer.add(expertContainer);
//

//            design.get(objVoteHeader.groupTo).grid.constraints.fillHorizontal = 1.5;

            design.getPanelContainer(objVote.groupTo).add(design.get(getPropertyDraw(generateVoteProject)));

            design.get(objProject.groupTo).grid.constraints.fillVertical = 1.5;
            design.get(objExpert.groupTo).grid.constraints.fillVertical = 1.5;

            design.setPanelLabelAbove(voteResultCommentGroup, true);
            design.setConstraintsFillHorizontal(voteResultCommentGroup, 1);

            design.setPreferredSize(voteResultCheckGroup, new Dimension(60, -1));

            design.get(objVote.groupTo).grid.hideToolbarItems();

            design.addIntersection(design.get(getPropertyDraw(innovativeCommentExpertVote)), design.get(getPropertyDraw(completeCommentExpertVote)), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.get(objProject.groupTo).grid.getContainer().setFixedSize(new Dimension(-1, 200));

            return design;
        }
    }

    private class GlobalFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private GlobalFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Глобальные параметры");

            addPropertyDraw(new LP[]{baseLM.currentDate, requiredPeriod, requiredQuantity, limitExperts, emailDocuments, emailClaimerFromAddress});
        }
    }

    private class VoteFormEntity extends FormEntity<SkolkovoBusinessLogics> {
        private ObjectEntity objVote;
        private ObjectEntity objExpert;

        private VoteFormEntity(NavigatorElement parent, String sID, boolean restricted) {
            super(parent, sID, (!restricted) ? "Реестр заседаний" : "Результаты заседаний");

            objVote = addSingleGroupObject(vote, nameNativeProjectVote, nameNativeClaimerVote, nameNativeClusterVote, equalsClusterProjectVote, dateStartVote, dateEndVote, openedVote, succeededVote, acceptedVote, quantityDoneVote, quantityInClusterVote, quantityInnovativeVote, quantityForeignVote);
            if (!restricted)
                addPropertyDraw(objVote, emailClosedVote, baseLM.delete);

            objExpert = addSingleGroupObject(expert);
            if (!restricted)
                addPropertyDraw(objExpert, baseLM.userFirstName, baseLM.userLastName, documentNameExpert, baseLM.userLogin, baseLM.userPassword, baseLM.email);

            addPropertyDraw(objExpert, objVote, oldExpertVote);
            addPropertyDraw(voteResultGroup, true, objExpert, objVote);
            if (!restricted)
                addPropertyDraw(objExpert, objVote, allowedEmailLetterExpertVote);
            setForceViewType(voteResultCommentGroup, ClassViewType.PANEL);

            if (!restricted) {
                addPropertyDraw(objVote, voteStartFormVote);
                addPropertyDraw(objVote, voteProtocolFormVote);
                setForceViewType(voteStartFormVote, ClassViewType.PANEL);
                setForceViewType(voteProtocolFormVote, ClassViewType.PANEL);

            }

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inExpertVote, objExpert, objVote)));
            
            RegularFilterGroupEntity voteFilterGroup = new RegularFilterGroupEntity(genID());
            voteFilterGroup.addFilter(new RegularFilterEntity(genID(),
                                                                new NotNullFilterEntity(addPropertyObject(openedVote, objVote)),
                                                                "Открыто",
                                                                KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)), !restricted);
            voteFilterGroup.addFilter(new RegularFilterEntity(genID(),
                                                                new NotNullFilterEntity(addPropertyObject(succeededVote, objVote)),
                                                                "Состоялось",
                                                                KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)), false);
            voteFilterGroup.addFilter(new RegularFilterEntity(genID(),
                                                                new NotNullFilterEntity(addPropertyObject(acceptedVote, objVote)),
                                                                "Положительно",
                                                                KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)), false);
            addRegularFilterGroup(voteFilterGroup);

            if (restricted) {
                addFixedFilter(new NotNullFilterEntity(addPropertyObject(voteResultExpertVote, objExpert, objVote)));
                setReadOnly(true);
            }
//            setReadOnly(true, objVote.groupTo);
//            setReadOnly(true, objExpert.groupTo);
//            setReadOnly(allowedEmailLetterExpertVote, false);
//            setReadOnly(emailClosedVote, false);

            setPageSize(0);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.setPanelLabelAbove(voteResultCommentGroup, true);
            design.setConstraintsFillHorizontal(voteResultCommentGroup, 0.5);

            design.setPreferredSize(voteResultCheckGroup, new Dimension(60, -1));

            design.get(objVote.groupTo).grid.getContainer().setFixedSize(new Dimension(-1, 300));

            return design;
        }
    }

    private class ExpertFormEntity extends FormEntity<SkolkovoBusinessLogics> {
        private ObjectEntity objExpert;
        private ObjectEntity objVote;
        private ObjectEntity objExtraCluster;

        private ExpertFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Реестр экспертов");

            objExpert = addSingleGroupObject(expert, baseLM.selection, baseLM.userFirstName, baseLM.userLastName, documentNameExpert, baseLM.userLogin, baseLM.userPassword, baseLM.email, disableExpert, nameNativeClusterExpert, nameLanguageExpert, expertResultGroup, baseLM.generateLoginPassword, emailAuthExpert);
            addObjectActions(this, objExpert);

            objVote = addSingleGroupObject(vote, nameNativeProjectVote, dateStartVote, dateEndVote, openedVote, succeededVote, quantityDoneVote);

            addPropertyDraw(voteResultGroup, true, objExpert, objVote);
            addPropertyDraw(objExpert, objVote, allowedEmailLetterExpertVote);
            setForceViewType(voteResultCommentGroup, ClassViewType.PANEL);

            objExtraCluster = addSingleGroupObject(cluster, "Дополнительные кластеры");
            addPropertyDraw(extraClusterExpert, objExtraCluster, objExpert);
            addPropertyDraw(objExtraCluster, nameNative, nameForeign);

            RegularFilterGroupEntity inactiveFilterGroup = new RegularFilterGroupEntity(genID());
            inactiveFilterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(disableExpert, objExpert))),
                    "Только активные"), true);
            addRegularFilterGroup(inactiveFilterGroup);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inNewExpertVote, objExpert, objVote)));

            addFixedFilter(new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(primClusterExpert, objExtraCluster, objExpert))));
//            setReadOnly(true, objVote.groupTo);
//            setReadOnly(allowedEmailLetterExpertVote, false);

            setPageSize(0);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.setPanelLabelAbove(voteResultCommentGroup, true);
            design.setConstraintsFillHorizontal(voteResultCommentGroup, 0.5);

            design.setPreferredSize(voteResultCheckGroup, new Dimension(60, -1));

            design.get(objExpert.groupTo).grid.getContainer().setFixedSize(new Dimension(-1, 300));

            ContainerView specContainer = design.createContainer();
            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objExpert.groupTo));
            specContainer.add(design.getGroupObjectContainer(objVote.groupTo));
            specContainer.add(design.getGroupObjectContainer(objExtraCluster.groupTo));
            specContainer.tabbedPane = true;

            return design;
        }
    }

    private class VoteExpertFormEntity extends FormEntity<SkolkovoBusinessLogics> {
        private ObjectEntity objExpert;
        private ObjectEntity objVote;

        private VoteExpertFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Реестр голосований");

            objVote = addSingleGroupObject(vote, nameNativeProjectVote, dateStartVote, dateEndVote, openedVote, succeededVote);

            objExpert = addSingleGroupObject(expert, baseLM.userFirstName, baseLM.userLastName, documentNameExpert, baseLM.email, nameNativeClusterExpert, nameLanguageExpert);

            addPropertyDraw(objExpert, objVote, allowedEmailLetterExpertVote);

            addPropertyDraw(voteResultGroup, true, objExpert, objVote);
            addPropertyDraw(expertResultGroup, true, objExpert);            
            setForceViewType(voteResultCommentGroup, ClassViewType.PANEL);

            GroupObjectEntity gobjVoteExpert = new GroupObjectEntity(genID());
            gobjVoteExpert.add(objVote);
            gobjVoteExpert.add(objExpert);
            addGroup(gobjVoteExpert);
            gobjVoteExpert.setSingleClassView(ClassViewType.GRID);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inNewExpertVote, objExpert, objVote)));
            
            RegularFilterGroupEntity filterGroupOpenedVote = new RegularFilterGroupEntity(genID());
            filterGroupOpenedVote.addFilter(new RegularFilterEntity(genID(),
                                  new NotNullFilterEntity(addPropertyObject(openedVote, objVote)),
                                  "Только открытые заседания",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            addRegularFilterGroup(filterGroupOpenedVote);

            RegularFilterGroupEntity filterGroupDoneExpertVote = new RegularFilterGroupEntity(genID());
            filterGroupDoneExpertVote.addFilter(new RegularFilterEntity(genID(),
                                  new CompareFilterEntity(addPropertyObject(doneExpertVote, objExpert, objVote), Compare.EQUALS, addPropertyObject(baseLM.vtrue)),
                                  "Только проголосовавшие",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            addRegularFilterGroup(filterGroupDoneExpertVote);

            RegularFilterGroupEntity filterGroupExpertVote = new RegularFilterGroupEntity(genID());
            filterGroupExpertVote.addFilter(new RegularFilterEntity(genID(),
                                  new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(voteResultExpertVote, objExpert, objVote))),
                                  "Только не принявшие участие",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
            addRegularFilterGroup(filterGroupExpertVote);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.setPanelLabelAbove(voteResultCommentGroup, true);
            design.setConstraintsFillHorizontal(voteResultCommentGroup, 0.5);

            return design;
        }
    }

    private class DocumentTemplateFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private DocumentTemplateFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Шаблоны документов");

            ObjectEntity objDocumentTemplate = addSingleGroupObject(documentTemplate, "Шаблон", baseLM.name);
            addObjectActions(this, objDocumentTemplate);

            ObjectEntity objDocumentTemplateDetail = addSingleGroupObject(documentTemplateDetail, nameTypeDocument, nameLanguageDocument);
            addObjectActions(this, objDocumentTemplateDetail);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(documentTemplateDocumentTemplateDetail, objDocumentTemplateDetail), Compare.EQUALS, objDocumentTemplate));
        }
    }

    private class ClaimerFormEntity extends FormEntity<SkolkovoBusinessLogics> {
         public ObjectEntity objClaimer;

        private ClaimerFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Реестр заявителей");

            objClaimer = addSingleGroupObject(claimer, "Заявитель", claimerInformationGroup, contactGroup, editClaimer);
        }
    }


    private class ClaimerFullFormEntity extends ClassFormEntity<SkolkovoBusinessLogics> {
         public ObjectEntity objClaimer;

         private ClaimerFullFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Заявители");

            objClaimer = addSingleGroupObject(claimer, "Заявитель", claimerInformationGroup, contactGroup, documentGroup, legalDataGroup);
            objClaimer.groupTo.setSingleClassView(ClassViewType.PANEL);
            editClaimer = addMFAProp(actionGroup, "Редактировать", this, new ObjectEntity[]{objClaimer}).setImage("edit.png");
         }

        @Override
        public ObjectEntity getObject() {
            return objClaimer;
        }
    }

    private class LanguageDocumentTypeFormEntity extends FormEntity<SkolkovoBusinessLogics> { // письмо эксперту
        private ObjectEntity objLanguage;
        private ObjectEntity objDocumentType;

        private GroupObjectEntity gobjLanguageDocumentType;

        private LanguageDocumentTypeFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Обязательные документы");

            gobjLanguageDocumentType = new GroupObjectEntity(genID());
            objLanguage = new ObjectEntity(genID(), language, "Язык");
            objDocumentType = new ObjectEntity(genID(), documentType, "Тип документа");
            gobjLanguageDocumentType.add(objLanguage);
            gobjLanguageDocumentType.add(objDocumentType);
            addGroup(gobjLanguageDocumentType);

            addPropertyDraw(objLanguage, baseLM.name);
            addPropertyDraw(objDocumentType, baseLM.name);
            addPropertyDraw(objLanguage, objDocumentType, quantityMinLanguageDocumentType, quantityMaxLanguageDocumentType, translateLanguageDocumentType);
        }
    }

    private class ExpertLetterFormEntity extends FormEntity<SkolkovoBusinessLogics> { // письмо эксперту
        private ObjectEntity objExpert;
        private ObjectEntity objVote;

        private ObjectEntity objDocument;

        private GroupObjectEntity gobjExpertVote;

        private ExpertLetterFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Письмо о заседании", true);

            gobjExpertVote = new GroupObjectEntity(2, "expertVote");
            objExpert = new ObjectEntity(2, "expert", expert, "Эксперт");
            objVote = new ObjectEntity(3, "vote", vote, "Заседание");
            gobjExpertVote.add(objExpert);
            gobjExpertVote.add(objVote);
            addGroup(gobjExpertVote);
            gobjExpertVote.initClassView = ClassViewType.PANEL;

            addPropertyDraw(baseLM.webHost, gobjExpertVote);
            addPropertyDraw(requiredPeriod, gobjExpertVote);
            addPropertyDraw(objExpert, baseLM.name, documentNameExpert, isForeignExpert, localeExpert);
            addPropertyDraw(objVote, nameNativeClaimerVote, nameForeignClaimerVote, nameNativeProjectVote, nameForeignProjectVote);
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inNewExpertVote, objExpert, objVote)));

            //!!!
            //dateStartVote = addJProp(baseGroup, "dateStartVote", "Дата начала", baseLM.and1, date, 1, is(vote), 1);
            //LP isDocumentUnique = addJProp(baseGroup,"isDocumentUnique", "уникальность док-та", baseLM.and1, languageDocument, is(), objDocument);

            objDocument = addSingleGroupObject(8, "document", document, fileDocument);
            addPropertyDraw(nameDocument, objDocument).setSID("docName");
            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectDocument, objDocument), Compare.EQUALS, addPropertyObject(projectVote, objVote)));
            addFixedFilter(new OrFilterEntity(new NotNullFilterEntity(addPropertyObject(inDocumentExpert, objDocument, objExpert)),
                                              new NotNullFilterEntity(addPropertyObject(inDefaultDocumentExpert, objDocument, objExpert))));

            addInlineEAForm(emailLetterExpertVoteEA, this, objExpert, 1, objVote, 2);
        }

        @Override
        public void modifyHierarchy(GroupObjectHierarchy groupHierarchy) {
            groupHierarchy.markGroupAsNonJoinable(gobjExpertVote);
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }
    }

    private class ExpertAuthFormEntity extends FormEntity<SkolkovoBusinessLogics> { // письмо эксперту о логине
        private ObjectEntity objExpert;

        private ExpertAuthFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Аутентификация эксперта", true);

            objExpert = addSingleGroupObject(1, "expert", expert, baseLM.userLogin, baseLM.userPassword, baseLM.name, documentNameExpert, isForeignExpert);
            objExpert.groupTo.initClassView = ClassViewType.PANEL;

            addInlineEAForm(emailAuthExpertEA, this, objExpert, 1);
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }
    }

    private class VoteStartFormEntity extends FormEntity<SkolkovoBusinessLogics> { // письмо эксперту

        private ObjectEntity objVote;

        private ObjectEntity objExpert;
        private ObjectEntity objOldExpert;

        private VoteStartFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Созыв заседания", true);

            objVote = addSingleGroupObject(1, "vote", vote, baseLM.date, dateProjectVote, nameNativeClaimerVote, nameNativeProjectVote, nameAblateClaimerVote, prevDateStartVote, prevDateVote);
            objVote.groupTo.initClassView = ClassViewType.PANEL;

            objExpert = addSingleGroupObject(2, "expert", expert, baseLM.userLastName, baseLM.userFirstName, documentNameExpert);
            addPropertyDraw(numberNewExpertVote, objExpert, objVote);
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inNewExpertVote, objExpert, objVote)));

            objOldExpert = addSingleGroupObject(3, "oldexpert", expert, baseLM.userLastName, baseLM.userFirstName, documentNameExpert);
            addPropertyDraw(numberOldExpertVote, objOldExpert, objVote);
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inOldExpertVote, objOldExpert, objVote)));

            addAttachEAForm(emailStartVoteEA, this, EmailActionProperty.Format.PDF, objVote, 1);
            addAttachEAForm(emailClosedVoteEA, this, EmailActionProperty.Format.PDF, emailStartHeaderVote, objVote, 1);

            voteStartFormVote = addFAProp("Созыв заседания", this, objVote);
        }

        @Override
        public void modifyHierarchy(GroupObjectHierarchy groupHierarchy) {
            groupHierarchy.markGroupAsNonJoinable(objVote.groupTo);
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }
    }

    private class VoteProtocolFormEntity extends FormEntity<SkolkovoBusinessLogics> { // письмо эксперту

        private ObjectEntity objVote;
        
        private ObjectEntity objExpert;

        private VoteProtocolFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Протокол заседания", true);

            objVote = addSingleGroupObject(1, "vote", vote, dateProjectVote, baseLM.date, dateEndVote, nameNativeProjectVote, nameNativeClaimerVote, nameNativeClusterVote, quantityInVote, quantityRepliedVote, quantityDoneVote, quantityDoneNewVote, quantityDoneOldVote, quantityRefusedVote, quantityConnectedVote, succeededVote, quantityInClusterVote, acceptedInClusterVote, quantityInnovativeVote, acceptedInnovativeVote, quantityForeignVote, acceptedForeignVote, nameStatusProjectVote, prevDateStartVote, prevDateVote);
            objVote.groupTo.initClassView = ClassViewType.PANEL;

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(closedVote, objVote)));

            objExpert = addSingleGroupObject(12, "expert", expert, baseLM.userFirstName, baseLM.userLastName, documentNameExpert);

            addPropertyDraw(voteResultGroup, true, objExpert, objVote);

            addPropertyDraw(connectedExpertVote, objExpert, objVote);

            addFixedFilter(new OrFilterEntity(new NotNullFilterEntity(addPropertyObject(doneExpertVote, objExpert, objVote)),
                                              new NotNullFilterEntity(addPropertyObject(connectedExpertVote, objExpert, objVote))));

            addAttachEAForm(emailProtocolVoteEA, this, EmailActionProperty.Format.PDF, objVote, 1);
            addAttachEAForm(emailClosedVoteEA, this, EmailActionProperty.Format.PDF, emailProtocolHeaderVote, objVote, 1);

            voteProtocolFormVote = addFAProp("Протокол заседания", this, objVote);
        }

        @Override
        public void modifyHierarchy(GroupObjectHierarchy groupHierarchy) {
            groupHierarchy.markGroupAsNonJoinable(objVote.groupTo);
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }
    }

    private class ExpertProtocolFormEntity extends FormEntity<SkolkovoBusinessLogics> { // письмо эксперту

        private ObjectEntity objDateFrom;
        private ObjectEntity objDateTo;

        private ObjectEntity objExpert;

        private ObjectEntity objVoteHeader;
        private ObjectEntity objVote;

        private ExpertProtocolFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Протокол голосования экспертов", true);

            GroupObjectEntity gobjDates = new GroupObjectEntity(1, "date");
            objDateFrom = new ObjectEntity(2, DateClass.instance, "Дата (с)");
            objDateTo = new ObjectEntity(3, DateClass.instance, "Дата (по)");
            gobjDates.add(objDateFrom);
            gobjDates.add(objDateTo);

            addGroup(gobjDates);
            gobjDates.setSingleClassView(ClassViewType.PANEL);

            addPropertyDraw(objDateFrom, baseLM.objectValue);
            getPropertyDraw(baseLM.objectValue, objDateFrom).setSID("dateFrom");

            // так делать неправильно в общем случае, поскольку getPropertyDraw ищет по groupObject, а не object

            addPropertyDraw(objDateTo, baseLM.objectValue);
            getPropertyDraw(baseLM.objectValue, objDateTo).setSID("dateTo");

            objExpert = addSingleGroupObject(4, "expert", expert, baseLM.userFirstName, baseLM.userLastName, documentNameExpert, nameNativeClusterExpert);
            objExpert.groupTo.initClassView = ClassViewType.PANEL;

            addPropertyDraw(objExpert, objDateFrom, objDateTo, quantityDoneExpertDateFromDateTo, quantityInExpertDateFromDateTo);

            objVoteHeader = addSingleGroupObject(5, "voteHeader", vote);

            objVote = addSingleGroupObject(6, "vote", vote, dateProjectVote, baseLM.date, dateEndVote, nameNativeProjectVote, nameNativeClaimerVote, nameNativeClusterVote);

            addPropertyDraw(nameNativeClaimerVote, objVoteHeader).setSID("nameNativeClaimerVoteHeader");
            addPropertyDraw(nameNativeProjectVote, objVoteHeader);

            addPropertyDraw(voteResultGroup, true, objExpert, objVote);

            addPropertyDraw(connectedExpertVote, objExpert, objVote);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(dateExpertVote, objExpert, objVoteHeader), Compare.GREATER_EQUALS, objDateFrom));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(dateExpertVote, objExpert, objVoteHeader), Compare.LESS_EQUALS, objDateTo));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(doneNewExpertVote, objExpert, objVoteHeader)));

            addFixedFilter(new CompareFilterEntity(addPropertyObject(dateExpertVote, objExpert, objVote), Compare.GREATER_EQUALS, objDateFrom));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(dateExpertVote, objExpert, objVote), Compare.LESS_EQUALS, objDateTo));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(doneNewExpertVote, objExpert, objVote)));

            RegularFilterGroupEntity filterGroupExpertVote = new RegularFilterGroupEntity(genID());
            filterGroupExpertVote.addFilter(new RegularFilterEntity(genID(),
                                  new NotNullFilterEntity(addPropertyObject(quantityDoneExpertDateFromDateTo, objExpert, objDateFrom, objDateTo)),
                                  "Голосовавшие",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)), true);
            filterGroupExpertVote.addFilter(new RegularFilterEntity(genID(),
                                  new NotNullFilterEntity(addPropertyObject(quantityInExpertDateFromDateTo, objExpert, objDateFrom, objDateTo)),
                                  "Учавствовавшие",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
            addRegularFilterGroup(filterGroupExpertVote);

            setReadOnly(true);
            setReadOnly(false, objDateFrom.groupTo);
        }
    }

    private class VoteClaimerFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objVote;

        private VoteClaimerFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);
                                    
            objVote = addSingleGroupObject(1, "vote", vote, "Заседание", prevDateVote);
            objVote.groupTo.initClassView = ClassViewType.PANEL;

            addInlineEAForm(emailClaimerVoteEA, this, objVote, 1);
        }
    }

    private class ClaimerAcceptedFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objVote;

        private ClaimerAcceptedFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Решение о соответствии", true);

            objVote = addSingleGroupObject(genID(), "vote", vote, "Заседание", dateEndVote, nameNativeProjectVote, dateProjectVote, nameNativeClaimerVote, nameAblateClaimerVote);
            objVote.groupTo.initClassView = ClassViewType.PANEL;

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(succeededVote, objVote)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(acceptedVote, objVote)));

            addAttachEAForm(emailClosedVoteEA, this, EmailActionProperty.Format.PDF, emailClaimerAcceptedHeaderVote, objVote, 1);
        }
    }

    private class ClaimerRejectedFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objVote;

        private ClaimerRejectedFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Решение о несоответствии", true);

            objVote = addSingleGroupObject(genID(), "vote", vote, "Заседание", dateEndVote, nameNativeProjectVote, dateProjectVote, nameNativeClaimerVote, nameAblateClaimerVote, nameDativusClaimerVote, nameGenitiveClaimerVote);
            objVote.groupTo.initClassView = ClassViewType.PANEL;

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(succeededVote, objVote)));
            addFixedFilter(new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(acceptedVote, objVote))));

            addAttachEAForm(emailClosedVoteEA, this, EmailActionProperty.Format.PDF, emailClaimerRejectedHeaderVote, objVote, 1);
        }
    }

     private class ClaimerStatusFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objProject;

        private ClaimerStatusFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Решение о присвоении статуса участника", true);

            objProject = addSingleGroupObject(genID(), "project", project, "Проект", baseLM.date, nameNativeProject, nameNativeClaimerProject, nameAblateClaimerProject, nameDativusClaimerProject, nameGenitiveClaimerProject);
            objProject.groupTo.initClassView = ClassViewType.PANEL;

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(acceptedProject, objProject)));

            addAttachEAForm(emailAcceptedProjectEA, this, EmailActionProperty.Format.PDF, emailClaimerAcceptedHeaderProject, objProject, 1);
        }
    }
    
    public class GenerateDocumentsActionProperty extends ActionProperty {

        private final ClassPropertyInterface projectInterface;
        private final ClassPropertyInterface documentTemplateInterface;

        public GenerateDocumentsActionProperty() {
            super(genSID(), "Сгенерировать документы", new ValueClass[]{project, documentTemplate});

            Iterator<ClassPropertyInterface> i = interfaces.iterator();
            projectInterface = i.next();
            documentTemplateInterface = i.next();
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
            DataObject projectObject = keys.get(projectInterface);
            DataObject documentTemplateObject = keys.get(documentTemplateInterface);

            Query<String, String> query = new Query<String, String>(Collections.singleton("key"));
            query.and(documentTemplateDocumentTemplateDetail.getExpr(modifier, query.mapKeys.get("key")).compare(documentTemplateObject.getExpr(), Compare.EQUALS));
            query.properties.put("documentType", typeDocument.getExpr(modifier, query.mapKeys.get("key")));
            query.properties.put("languageDocument", languageDocument.getExpr(modifier, query.mapKeys.get("key")));

            for (Map<String, Object> row : query.execute(session.sql, session.env).values()) {
                DataObject documentObject = session.addObject(document, modifier);
                projectDocument.execute(projectObject.getValue(), session, modifier, documentObject);
                typeDocument.execute(row.get("documentType"), session, modifier, documentObject);
                languageDocument.execute(row.get("languageDocument"), session, modifier, documentObject);
            }
        }
    }



    public class IncludeDocumentsActionProperty extends ActionProperty {

        private final ClassPropertyInterface projectInterface;
        private final ClassPropertyInterface documentTemplateInterface;

        public IncludeDocumentsActionProperty() {
            super(genSID(), "Подключить документы", new ValueClass[]{project, documentTemplate});

            Iterator<ClassPropertyInterface> i = interfaces.iterator();
            projectInterface = i.next();
            documentTemplateInterface = i.next();
        }


        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            throw new RuntimeException("no need");
        }

        @Override
        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {

            try {

                DataObject projectObject = keys.get(projectInterface);

                RemoteFormInterface remoteForm = executeForm.createForm(projectFullNative, Collections.singletonMap(projectFullNative.objProject, projectObject));
                ReportGenerator report = new ReportGenerator(remoteForm);
                JasperPrint print = report.createReport(false, false, new HashMap());
                JRAbstractExporter exporter = new JRPdfExporter();
                File tempFile = File.createTempFile("lsfReport", ".pdf");
                exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, tempFile.getAbsolutePath());
                exporter.setParameter(JRExporterParameter.JASPER_PRINT, print);
                exporter.exportReport();
                byte[] fileBytes = IOUtils.getFileBytes(tempFile);

                DataObject documentObject = session.addObject(document, session.modifier);
                projectDocument.execute(projectObject.getValue(), session, documentObject);
                typeDocument.execute(documentType.getID("application"), session, documentObject);
                languageDocument.execute(language.getID("russian"), session, documentObject);
                fileDocument.execute(fileBytes, session, documentObject);


                remoteForm = executeForm.createForm(projectFullForeign, Collections.singletonMap(projectFullForeign.objProject, projectObject));
                report = new ReportGenerator(remoteForm);
                print = report.createReport(false, false, new HashMap());
                exporter = new JRPdfExporter();
                tempFile = File.createTempFile("lsfReport", ".pdf");
                exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, tempFile.getAbsolutePath());
                exporter.setParameter(JRExporterParameter.JASPER_PRINT, print);
                exporter.exportReport();
                fileBytes = IOUtils.getFileBytes(tempFile);

                documentObject = session.addObject(document, session.modifier);
                projectDocument.execute(projectObject.getValue(), session, documentObject);
                typeDocument.execute(documentType.getID("application"), session, documentObject);
                languageDocument.execute(language.getID("english"), session, documentObject);
                fileDocument.execute(fileBytes, session, documentObject);


                Object file = fileNativeSummaryProject.read(session, projectObject);
                if (file != null) {
                    documentObject = session.addObject(document, session.modifier);
                    fileDocument.execute(file, session, documentObject);
                    projectDocument.execute(projectObject.getValue(), session, documentObject);
                    typeDocument.execute(documentType.getID("resume"), session, documentObject);
                    languageDocument.execute(language.getID("russian"), session, documentObject);
                }

                file = fileForeignSummaryProject.read(session, projectObject);
                if (file != null) {
                    documentObject = session.addObject(document, session.modifier);
                    fileDocument.execute(file, session, documentObject);
                    projectDocument.execute(projectObject.getValue(), session, documentObject);
                    typeDocument.execute(documentType.getID("resume"), session, documentObject);
                    languageDocument.execute(language.getID("english"), session, documentObject);
                }

                Query<String, String> query = new Query<String, String>(Collections.singleton("nonRussianSpecialist"));
                query.and(projectNonRussianSpecialist.getExpr(session.modifier, query.mapKeys.get("nonRussianSpecialist")).compare(projectObject.getExpr(), Compare.EQUALS));
                query.properties.put("fullNameNonRussianSpecialist", projectNonRussianSpecialist.getExpr(session.modifier, query.mapKeys.get("nonRussianSpecialist")));
                query.properties.put("fileForeignResumeNonRussianSpecialist", fileForeignResumeNonRussianSpecialist.getExpr(session.modifier, query.mapKeys.get("nonRussianSpecialist")));
                for (Map.Entry<Map<String, DataObject>, Map<String, ObjectValue>> row : query.executeClasses(session.sql, session.env, baseClass).entrySet()) {
                    row.getKey().get("nonRussianSpecialist");
                    row.getValue().get("fullNameNonRussianSpecialist");
                    row.getValue().get("fileForeignResumeNonRussianSpecialist");
                    documentObject = session.addObject(document, session.modifier);
                    projectDocument.execute(projectObject.getValue(), session, documentObject);
                    typeDocument.execute(documentType.getID("forres"), session, documentObject);
                    languageDocument.execute(language.getID("english"), session, documentObject);
                    fileDocument.execute(row.getValue().get("fileForeignResumeNonRussianSpecialist").getValue(), session, documentObject);
                }

                file = fileNativeTechnicalDescriptionProject.read(session, projectObject);
                if (file != null) {
                    documentObject = session.addObject(document, session.modifier);
                    projectDocument.execute(projectObject.getValue(), session, documentObject);
                    typeDocument.execute(documentType.getID("techdesc"), session, documentObject);
                    languageDocument.execute(language.getID("russian"), session, documentObject);
                    fileDocument.execute(file, session, documentObject);
                }

                file = fileForeignTechnicalDescriptionProject.read(session, projectObject);
                if (file != null) {
                    documentObject = session.addObject(document, session.modifier);
                    projectDocument.execute(projectObject.getValue(), session, documentObject);
                    typeDocument.execute(documentType.getID("techdesc"), session, documentObject);
                    languageDocument.execute(language.getID("english"), session, documentObject);
                    fileDocument.execute(file, session, documentObject);
                }

                file = fileRoadMapProject.read(session, projectObject);
                if (file != null) {
                    documentObject = session.addObject(document, session.modifier);
                    projectDocument.execute(projectObject.getValue(), session, documentObject);
                    typeDocument.execute(documentType.getID("techdesc"), session, documentObject);
                    languageDocument.execute(language.getID("russian"), session, documentObject);
                    fileDocument.execute(file, session, documentObject);
                }

            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (JRException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (ClassNotFoundException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }


    public class GenerateVoteActionProperty extends ActionProperty {

        private final ClassPropertyInterface projectInterface;

        public GenerateVoteActionProperty() {
            super(genSID(), "Сгенерировать заседание", new ValueClass[]{project});

            Iterator<ClassPropertyInterface> i = interfaces.iterator();
            projectInterface = i.next();
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
            DataObject projectObject = keys.get(projectInterface);

            // считываем всех экспертов, которые уже голосовали по проекту
            Query<String, String> query = new Query<String, String>(Collections.singleton("key"));
            query.and(doneProjectExpert.getExpr(modifier, projectObject.getExpr(), query.mapKeys.get("key")).getWhere());
            query.properties.put("vote", voteProjectExpert.getExpr(modifier, projectObject.getExpr(), query.mapKeys.get("key")));

            Map<DataObject, DataObject> previousResults = new HashMap<DataObject, DataObject>();
            for (Map.Entry<Map<String, DataObject>, Map<String, ObjectValue>> row : query.executeClasses(session.sql, session.env, baseClass).entrySet()) {
                previousResults.put(row.getKey().get("key"), (DataObject) row.getValue().get("vote"));
            }

            // считываем всех неголосовавших экспертов из этого кластера
            query = new Query<String, String>(Collections.singleton("key"));
            query.and(inClusterExpert.getExpr(modifier, clusterProject.getExpr(modifier, projectObject.getExpr()), query.mapKeys.get("key")).getWhere());
            query.and(disableExpert.getExpr(modifier, query.mapKeys.get("key")).getWhere().not());
            query.and(voteResultProjectExpert.getExpr(modifier, projectObject.getExpr(), query.mapKeys.get("key")).getWhere().not());

            query.properties.put("in", inProjectExpert.getExpr(modifier, projectObject.getExpr(), query.mapKeys.get("key")));

            // получаем два списка - один, которые уже назначались на проект, другой - которые нет
            java.util.List<DataObject> expertNew = new ArrayList<DataObject>();
            java.util.List<DataObject> expertVoted = new ArrayList<DataObject>();
            for (Map.Entry<Map<String, DataObject>, Map<String, ObjectValue>> row : query.executeClasses(session.sql, session.env, baseClass).entrySet()) {
                if (row.getValue().get("in").getValue() != null) // эксперт уже голосовал
                    expertVoted.add(row.getKey().get("key"));
                else
                    expertNew.add(row.getKey().get("key"));
            }

            Integer required = nvl((Integer) requiredQuantity.read(session, modifier), 0) - previousResults.size();
            if (required > expertVoted.size() + expertNew.size()) {
                actions.add(new MessageClientAction("Недостаточно экспертов по кластеру", "Генерация заседания"));
                return;
            }

            // создаем новое заседание
            DataObject voteObject;
            if (executeForm.form == null)
                voteObject = session.addObject(vote, modifier);
            else
                voteObject = executeForm.form.addObject(vote);
            projectVote.execute(projectObject.object, session, modifier, voteObject);

            // копируем результаты старых заседаний
            for (Map.Entry<DataObject, DataObject> row : previousResults.entrySet()) {
                inExpertVote.execute(true, session, modifier, row.getKey(), voteObject);
                oldExpertVote.execute(true, session, modifier, row.getKey(), voteObject);
                LP[] copyProperties = new LP[] {dateExpertVote, voteResultExpertVote, inClusterExpertVote,
                                                innovativeExpertVote, foreignExpertVote, innovativeCommentExpertVote,
                                                competentExpertVote, completeExpertVote, completeCommentExpertVote};
                for (LP property : copyProperties) {
                    property.execute(property.read(session, modifier, row.getKey(), row.getValue()), session, modifier, row.getKey(), voteObject);
                }
            }

            // назначаем новых экспертов - сначала, которые не голосовали еще, а затем остальных
            Random rand = new Random();
            while (required > 0) {
                if (!expertNew.isEmpty())
                    inExpertVote.execute(true, session, modifier, expertNew.remove(rand.nextInt(expertNew.size())), voteObject);
                else
                    inExpertVote.execute(true, session, modifier, expertVoted.remove(rand.nextInt(expertVoted.size())), voteObject);
                required--;
            }
        }

        @Override
        public Set<Property> getChangeProps() {
            return BaseUtils.toSet(projectVote.property, inExpertVote.property);
        }
    }

    public class CopyResultsActionProperty extends ActionProperty {

        private final ClassPropertyInterface voteInterface;

        public CopyResultsActionProperty() {
            super(genSID(), "Скопировать результаты из предыдущего заседания", new ValueClass[]{vote});

            Iterator<ClassPropertyInterface> i = interfaces.iterator();
            voteInterface = i.next();
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            throw new RuntimeException("no need");
        }

        @Override
        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
            DataObject voteObject = keys.get(voteInterface);
            java.sql.Date dateStart = (java.sql.Date) dateStartVote.read(session, modifier, voteObject);

            DataObject projectObject = new DataObject(projectVote.read(session, modifier, voteObject), project);
            Query<String, String> voteQuery = new Query<String, String>(Collections.singleton("vote"));
            voteQuery.and(projectVote.getExpr(modifier, voteQuery.mapKeys.get("vote")).compare(projectObject.getExpr(), Compare.EQUALS));
            voteQuery.properties.put("dateStartVote", dateStartVote.getExpr(modifier, voteQuery.mapKeys.get("vote")));

            java.sql.Date datePrev = null;
            DataObject votePrevObject = null;

            for (Map.Entry<Map<String, DataObject>, Map<String, ObjectValue>> row : voteQuery.executeClasses(session.sql, session.env, baseClass).entrySet()) {
                java.sql.Date dateCur = (java.sql.Date) row.getValue().get("dateStartVote").getValue();
                if (dateCur != null && dateCur.getTime() < dateStart.getTime() && (datePrev == null || dateCur.getTime() > datePrev.getTime())) {
                    datePrev = dateCur;
                    votePrevObject = row.getKey().get("vote");
                }
            }
            if (votePrevObject == null) return;

            // считываем всех экспертов, которые уже голосовали по проекту
            Query<String, String> query = new Query<String, String>(Collections.singleton("key"));
            query.and(doneExpertVote.getExpr(modifier, query.mapKeys.get("key"), votePrevObject.getExpr()).getWhere());
//            query.properties.put("expert", object(expert).getExpr(session.modifier, query.mapKeys.get("key")));

            Set<DataObject> experts = new HashSet<DataObject>();
            for (Map.Entry<Map<String, DataObject>, Map<String, ObjectValue>> row : query.executeClasses(session.sql, session.env, baseClass).entrySet()) {
                experts.add(row.getKey().get("key"));
            }

            // копируем результаты старых заседаний
            for (DataObject expert : experts) {
                inExpertVote.execute(true, session, modifier, expert, voteObject);
                oldExpertVote.execute(true, session, modifier, expert, voteObject);
                LP[] copyProperties = new LP[] {dateExpertVote, voteResultExpertVote, inClusterExpertVote,
                                                innovativeExpertVote, foreignExpertVote, innovativeCommentExpertVote,
                                                competentExpertVote, completeExpertVote, completeCommentExpertVote};
                for (LP property : copyProperties) {
                    property.execute(property.read(session, modifier, expert, votePrevObject), session, modifier, expert, voteObject);
                }
            }
        }
    }
}
