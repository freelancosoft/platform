package skolkovo.actions;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.JDOMParseException;
import org.jdom.input.SAXBuilder;
import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.base.OrderedMap;
import platform.interop.action.ClientAction;
import platform.interop.action.MessageClientAction;
import platform.server.classes.FileActionClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.integration.*;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LP;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.Modifier;
import skolkovo.SkolkovoLogicsModule;

import java.io.*;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.*;
import java.util.Date;

public class ImportProjectsActionProperty extends ActionProperty {

    private SkolkovoLogicsModule LM;
    private DataSession session;
    private static Logger logger = Logger.getLogger("import");
    private byte[] responseContents;
    private String toLog = "";
    private boolean onlyMessage;
    private boolean fillSids;

    public ImportProjectsActionProperty(String caption, SkolkovoLogicsModule LM, boolean onlyMessage, boolean fillSids) {
        super(LM.genSID(), caption, new ValueClass[]{});
        this.LM = LM;
        this.onlyMessage = onlyMessage;
        this.fillSids = fillSids;
    }

    protected ImportField dateProjectField,
            projectIdField, nameNativeProjectField, nameForeignProjectField,
            nameNativeManagerProjectField, nameNativeGenitiveManagerProjectField, nameNativeDativusManagerProjectField, nameNativeAblateManagerProjectField,
            nameForeignManagerProjectField,
            nativeProblemProjectField, foreignProblemProjectField, nativeInnovativeProjectField, foreignInnovativeProjectField,
            projectTypeProjectField, projectActionProjectField,
            nativeSubstantiationProjectTypeField, foreignSubstantiationProjectTypeField, nativeSubstantiationProjectClusterField, foreignSubstantiationProjectClusterField,
            isOwnedEquipmentProjectField, isAvailableEquipmentProjectField,
            isTransferEquipmentProjectField, descriptionTransferEquipmentProjectField, ownerEquipmentProjectField,
            isPlanningEquipmentProjectField, specificationEquipmentProjectField,
            isSeekEquipmentProjectField, descriptionEquipmentProjectField,
            isOtherEquipmentProjectField, commentEquipmentProjectField,

    isReturnInvestmentsProjectField, nameReturnInvestorProjectField, amountReturnFundsProjectField,
            isNonReturnInvestmentsProjectField, isCapitalInvestmentProjectField, isPropertyInvestmentProjectField, isGrantsProjectField, isOtherNonReturnInvestmentsProjectField,
            nameNonReturnInvestorProjectField, amountNonReturnFundsProjectField, commentOtherNonReturnInvestmentsProjectField,
            isOwnFundsProjectField, amountOwnFundsProjectField,
            isPlanningSearchSourceProjectField, amountFundsProjectField,
            isOtherSoursesProjectField, commentOtherSoursesProjectField, updateDateProjectField,

    nameNativeClusterField, inProjectClusterField, numberCurrentClusterField,
            nameNativeClaimerField, nameForeignClaimerField,
            firmNameNativeClaimerField, firmNameForeignClaimerField, phoneClaimerField, addressClaimerField, siteClaimerField,
            emailClaimerField, emailFirmClaimerField, OGRNClaimerField, INNClaimerField,
            fileStatementClaimerField, fileConstituentClaimerField, fileExtractClaimerField,
            fileNativeSummaryProjectField, fileForeignSummaryProjectField,
            fileRoadMapProjectField,
            nativeTypePatentField, foreignTypePatentField, nativeNumberPatentField, foreignNumberPatentField,
            datePatentField, isOwnedPatentField, ownerPatentField, ownerTypePatentField, isValuatedPatentField, valuatorPatentField,
            fileIntentionOwnerPatentField, fileActValuationPatentField,
            fullNameAcademicField, institutionAcademicField, titleAcademicField,
            fileDocumentConfirmingAcademicField, fileDocumentEmploymentAcademicField,
            fullNameNonRussianSpecialistField, organizationNonRussianSpecialistField, titleNonRussianSpecialistField,
            fileForeignResumeNonRussianSpecialistField, fileNativeResumeNonRussianSpecialistField,
            filePassportNonRussianSpecialistField, fileStatementNonRussianSpecialistField,
            fileNativeTechnicalDescriptionProjectField, fileForeignTechnicalDescriptionProjectField;

    ImportKey<?> projectKey, projectTypeProjectKey, projectActionProjectKey, claimerKey, patentKey, ownerTypePatentKey, clusterKey, academicKey, nonRussianSpecialistKey;
    List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesCluster = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesPatent = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesAcademic = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesNonRussianSpecialist = new ArrayList<ImportProperty<?>>();

    private void initFieldsNProperties() {
        dateProjectField = new ImportField(LM.baseLM.date);
        projectIdField = new ImportField(LM.sidProject);
        nameNativeProjectField = new ImportField(LM.nameNative);
        nameForeignProjectField = new ImportField(LM.nameForeign);
        nameNativeManagerProjectField = new ImportField(LM.nameNativeManagerProject);
        nameNativeGenitiveManagerProjectField = new ImportField(LM.nameNativeGenitiveManagerProject);
        nameNativeDativusManagerProjectField = new ImportField(LM.nameNativeDativusManagerProject);
        nameNativeAblateManagerProjectField = new ImportField(LM.nameNativeAblateManagerProject);
        nameForeignManagerProjectField = new ImportField(LM.nameForeignManagerProject);
        nativeProblemProjectField = new ImportField(LM.nativeProblemProject);
        foreignProblemProjectField = new ImportField(LM.foreignProblemProject);
        nativeInnovativeProjectField = new ImportField(LM.nativeInnovativeProject);
        foreignInnovativeProjectField = new ImportField(LM.foreignInnovativeProject);
        projectTypeProjectField = new ImportField(LM.baseLM.classSID);
        projectActionProjectField = new ImportField(LM.baseLM.classSID);
        nativeSubstantiationProjectTypeField = new ImportField(LM.nativeSubstantiationProjectType);
        foreignSubstantiationProjectTypeField = new ImportField(LM.foreignSubstantiationProjectType);
        isOwnedEquipmentProjectField = new ImportField(LM.isOwnedEquipmentProject);
        isAvailableEquipmentProjectField = new ImportField(LM.isAvailableEquipmentProject);
        isTransferEquipmentProjectField = new ImportField(LM.isTransferEquipmentProject);
        descriptionTransferEquipmentProjectField = new ImportField(LM.descriptionTransferEquipmentProject);
        ownerEquipmentProjectField = new ImportField(LM.ownerEquipmentProject);
        isPlanningEquipmentProjectField = new ImportField(LM.isPlanningEquipmentProject);
        specificationEquipmentProjectField = new ImportField(LM.specificationEquipmentProject);
        isSeekEquipmentProjectField = new ImportField(LM.isSeekEquipmentProject);
        descriptionEquipmentProjectField = new ImportField(LM.descriptionEquipmentProject);
        isOtherEquipmentProjectField = new ImportField(LM.isOtherEquipmentProject);
        commentEquipmentProjectField = new ImportField(LM.commentEquipmentProject);

        isReturnInvestmentsProjectField = new ImportField(LM.isReturnInvestmentsProject);
        nameReturnInvestorProjectField = new ImportField(LM.nameReturnInvestorProject);
        amountReturnFundsProjectField = new ImportField(LM.amountReturnFundsProject);
        isNonReturnInvestmentsProjectField = new ImportField(LM.isNonReturnInvestmentsProject);
        isCapitalInvestmentProjectField = new ImportField(LM.isCapitalInvestmentProject);
        isPropertyInvestmentProjectField = new ImportField(LM.isPropertyInvestmentProject);
        isGrantsProjectField = new ImportField(LM.isGrantsProject);
        isOtherNonReturnInvestmentsProjectField = new ImportField(LM.isOtherNonReturnInvestmentsProject);

        nameNonReturnInvestorProjectField = new ImportField(LM.nameNonReturnInvestorProject);
        amountNonReturnFundsProjectField = new ImportField(LM.amountNonReturnFundsProject);
        commentOtherNonReturnInvestmentsProjectField = new ImportField(LM.commentOtherNonReturnInvestmentsProject);
        isOwnFundsProjectField = new ImportField(LM.isOwnFundsProject);
        amountOwnFundsProjectField = new ImportField(LM.amountOwnFundsProject);
        isPlanningSearchSourceProjectField = new ImportField(LM.isPlanningSearchSourceProject);
        amountFundsProjectField = new ImportField(LM.amountFundsProject);
        isOtherSoursesProjectField = new ImportField(LM.isOtherSoursesProject);
        commentOtherSoursesProjectField = new ImportField(LM.commentOtherSoursesProject);
        updateDateProjectField = new ImportField(LM.updateDateProject);

        nameNativeClusterField = new ImportField(LM.nameNativeCluster);
        inProjectClusterField = new ImportField(LM.inProjectCluster);
        numberCurrentClusterField = new ImportField(LM.numberCluster);
        nativeSubstantiationProjectClusterField = new ImportField(LM.nativeSubstantiationProjectCluster);
        foreignSubstantiationProjectClusterField = new ImportField(LM.foreignSubstantiationProjectCluster);

        nameNativeClaimerField = new ImportField(LM.nameNativeClaimer);
        nameForeignClaimerField = new ImportField(LM.nameForeignClaimer);
        firmNameNativeClaimerField = new ImportField(LM.firmNameNativeClaimer);
        firmNameForeignClaimerField = new ImportField(LM.firmNameForeignClaimer);
        phoneClaimerField = new ImportField(LM.phoneClaimer);
        addressClaimerField = new ImportField(LM.addressClaimer);
        siteClaimerField = new ImportField(LM.siteClaimer);
        emailClaimerField = new ImportField(LM.emailClaimer);
        emailFirmClaimerField = new ImportField(LM.emailFirmClaimer);
        OGRNClaimerField = new ImportField(LM.OGRNClaimer);
        INNClaimerField = new ImportField(LM.INNClaimer);
        fileStatementClaimerField = new ImportField(LM.statementClaimer);
        fileConstituentClaimerField = new ImportField(LM.constituentClaimer);
        fileExtractClaimerField = new ImportField(LM.extractClaimer);
        fileNativeSummaryProjectField = new ImportField(LM.fileNativeSummaryProject);
        fileForeignSummaryProjectField = new ImportField(LM.fileForeignSummaryProject);
        fileRoadMapProjectField = new ImportField(LM.fileRoadMapProject);
        fileNativeTechnicalDescriptionProjectField = new ImportField(LM.fileNativeTechnicalDescriptionProject);
        fileForeignTechnicalDescriptionProjectField = new ImportField(LM.fileForeignTechnicalDescriptionProject);

        nativeTypePatentField = new ImportField(LM.nativeTypePatent);
        foreignTypePatentField = new ImportField(LM.foreignTypePatent);
        nativeNumberPatentField = new ImportField(LM.nativeNumberPatent);
        foreignNumberPatentField = new ImportField(LM.foreignNumberPatent);
        datePatentField = new ImportField(LM.priorityDatePatent);
        isOwnedPatentField = new ImportField(LM.isOwned);
        ownerPatentField = new ImportField(LM.ownerPatent);
        ownerTypePatentField = new ImportField(LM.baseLM.classSID);
        isValuatedPatentField = new ImportField(LM.isValuated);
        valuatorPatentField = new ImportField(LM.valuatorPatent);
        fileIntentionOwnerPatentField = new ImportField(LM.fileIntentionOwnerPatent);
        fileActValuationPatentField = new ImportField(LM.fileActValuationPatent);

        fullNameAcademicField = new ImportField(LM.fullNameAcademic);
        institutionAcademicField = new ImportField(LM.institutionAcademic);
        titleAcademicField = new ImportField(LM.titleAcademic);
        fileDocumentConfirmingAcademicField = new ImportField(LM.fileDocumentConfirmingAcademic);
        fileDocumentEmploymentAcademicField = new ImportField(LM.fileDocumentEmploymentAcademic);

        fullNameNonRussianSpecialistField = new ImportField(LM.fullNameNonRussianSpecialist);
        organizationNonRussianSpecialistField = new ImportField(LM.organizationNonRussianSpecialist);
        titleNonRussianSpecialistField = new ImportField(LM.titleNonRussianSpecialist);
        fileForeignResumeNonRussianSpecialistField = new ImportField(LM.fileForeignResumeNonRussianSpecialist);
        fileNativeResumeNonRussianSpecialistField = new ImportField(LM.fileNativeResumeNonRussianSpecialist);
        filePassportNonRussianSpecialistField = new ImportField(LM.filePassportNonRussianSpecialist);
        fileStatementNonRussianSpecialistField = new ImportField(LM.fileStatementNonRussianSpecialist);


        projectKey = new ImportKey(LM.project, LM.sidToProject.getMapping(projectIdField));
        properties.add(new ImportProperty(dateProjectField, LM.dateProject.getMapping(projectKey)));
        properties.add(new ImportProperty(projectIdField, LM.sidProject.getMapping(projectKey)));
        properties.add(new ImportProperty(nameNativeProjectField, LM.nameNativeProject.getMapping(projectKey)));
        properties.add(new ImportProperty(nameForeignProjectField, LM.nameForeignProject.getMapping(projectKey)));
        properties.add(new ImportProperty(nameNativeManagerProjectField, LM.nameNativeManagerProject.getMapping(projectKey)));
        properties.add(new ImportProperty(nameNativeGenitiveManagerProjectField, LM.nameNativeGenitiveManagerProject.getMapping(projectKey)));
        properties.add(new ImportProperty(nameNativeDativusManagerProjectField, LM.nameNativeDativusManagerProject.getMapping(projectKey)));
        properties.add(new ImportProperty(nameNativeAblateManagerProjectField, LM.nameNativeAblateManagerProject.getMapping(projectKey)));
        properties.add(new ImportProperty(nameForeignManagerProjectField, LM.nameForeignManagerProject.getMapping(projectKey)));
        properties.add(new ImportProperty(nativeProblemProjectField, LM.nativeProblemProject.getMapping(projectKey)));
        properties.add(new ImportProperty(foreignProblemProjectField, LM.foreignProblemProject.getMapping(projectKey)));
        properties.add(new ImportProperty(nativeInnovativeProjectField, LM.nativeInnovativeProject.getMapping(projectKey)));
        properties.add(new ImportProperty(foreignInnovativeProjectField, LM.foreignInnovativeProject.getMapping(projectKey)));
        properties.add(new ImportProperty(nativeSubstantiationProjectTypeField, LM.nativeSubstantiationProjectType.getMapping(projectKey)));
        properties.add(new ImportProperty(foreignSubstantiationProjectTypeField, LM.foreignSubstantiationProjectType.getMapping(projectKey)));

        properties.add(new ImportProperty(isOwnedEquipmentProjectField, LM.isOwnedEquipmentProject.getMapping(projectKey)));
        properties.add(new ImportProperty(isAvailableEquipmentProjectField, LM.isAvailableEquipmentProject.getMapping(projectKey)));
        properties.add(new ImportProperty(isTransferEquipmentProjectField, LM.isTransferEquipmentProject.getMapping(projectKey)));
        properties.add(new ImportProperty(descriptionTransferEquipmentProjectField, LM.descriptionTransferEquipmentProject.getMapping(projectKey)));
        properties.add(new ImportProperty(ownerEquipmentProjectField, LM.ownerEquipmentProject.getMapping(projectKey)));
        properties.add(new ImportProperty(isPlanningEquipmentProjectField, LM.isPlanningEquipmentProject.getMapping(projectKey)));
        properties.add(new ImportProperty(specificationEquipmentProjectField, LM.specificationEquipmentProject.getMapping(projectKey)));
        properties.add(new ImportProperty(isSeekEquipmentProjectField, LM.isSeekEquipmentProject.getMapping(projectKey)));
        properties.add(new ImportProperty(descriptionEquipmentProjectField, LM.descriptionEquipmentProject.getMapping(projectKey)));
        properties.add(new ImportProperty(isOtherEquipmentProjectField, LM.isOtherEquipmentProject.getMapping(projectKey)));
        properties.add(new ImportProperty(commentEquipmentProjectField, LM.commentEquipmentProject.getMapping(projectKey)));

        properties.add(new ImportProperty(isReturnInvestmentsProjectField, LM.isReturnInvestmentsProject.getMapping(projectKey)));
        properties.add(new ImportProperty(nameReturnInvestorProjectField, LM.nameReturnInvestorProject.getMapping(projectKey)));
        properties.add(new ImportProperty(amountReturnFundsProjectField, LM.amountReturnFundsProject.getMapping(projectKey)));
        properties.add(new ImportProperty(isNonReturnInvestmentsProjectField, LM.isNonReturnInvestmentsProject.getMapping(projectKey)));
        properties.add(new ImportProperty(nameNonReturnInvestorProjectField, LM.nameNonReturnInvestorProject.getMapping(projectKey)));
        properties.add(new ImportProperty(amountNonReturnFundsProjectField, LM.amountNonReturnFundsProject.getMapping(projectKey)));
        properties.add(new ImportProperty(commentOtherNonReturnInvestmentsProjectField, LM.commentOtherNonReturnInvestmentsProject.getMapping(projectKey)));
        properties.add(new ImportProperty(isCapitalInvestmentProjectField, LM.isCapitalInvestmentProject.getMapping(projectKey)));
        properties.add(new ImportProperty(isPropertyInvestmentProjectField, LM.isPropertyInvestmentProject.getMapping(projectKey)));
        properties.add(new ImportProperty(isGrantsProjectField, LM.isGrantsProject.getMapping(projectKey)));
        properties.add(new ImportProperty(isOtherNonReturnInvestmentsProjectField, LM.isOtherNonReturnInvestmentsProject.getMapping(projectKey)));
        properties.add(new ImportProperty(isOwnFundsProjectField, LM.isOwnFundsProject.getMapping(projectKey)));
        properties.add(new ImportProperty(amountOwnFundsProjectField, LM.amountOwnFundsProject.getMapping(projectKey)));
        properties.add(new ImportProperty(isPlanningSearchSourceProjectField, LM.isPlanningSearchSourceProject.getMapping(projectKey)));
        properties.add(new ImportProperty(amountFundsProjectField, LM.amountFundsProject.getMapping(projectKey)));
        properties.add(new ImportProperty(isOtherSoursesProjectField, LM.isOtherSoursesProject.getMapping(projectKey)));
        properties.add(new ImportProperty(commentOtherSoursesProjectField, LM.commentOtherSoursesProject.getMapping(projectKey)));
        properties.add(new ImportProperty(updateDateProjectField, LM.updateDateProject.getMapping(projectKey)));

        projectTypeProjectKey = new ImportKey(LM.projectType, LM.projectTypeToSID.getMapping(projectTypeProjectField));
        properties.add(new ImportProperty(projectTypeProjectField, LM.projectTypeProject.getMapping(projectKey),
                LM.baseLM.object(LM.projectType).getMapping(projectTypeProjectKey)));

        projectActionProjectKey = new ImportKey(LM.projectAction, LM.projectActionToSID.getMapping(projectActionProjectField));
        properties.add(new ImportProperty(projectActionProjectField, LM.projectActionProject.getMapping(projectKey),
                LM.baseLM.object(LM.projectAction).getMapping(projectActionProjectKey)));

        claimerKey = new ImportKey(LM.claimer, LM.nameNativeToClaimer.getMapping(nameNativeClaimerField));
        properties.add(new ImportProperty(nameNativeClaimerField, LM.claimerProject.getMapping(projectKey),
                LM.baseLM.object(LM.claimer).getMapping(claimerKey)));
        properties.add(new ImportProperty(nameNativeClaimerField, LM.nameNativeClaimer.getMapping(claimerKey)));
        properties.add(new ImportProperty(nameForeignClaimerField, LM.nameForeignClaimer.getMapping(claimerKey)));
        properties.add(new ImportProperty(firmNameNativeClaimerField, LM.firmNameNativeClaimer.getMapping(claimerKey)));
        properties.add(new ImportProperty(firmNameForeignClaimerField, LM.firmNameForeignClaimer.getMapping(claimerKey)));
        properties.add(new ImportProperty(phoneClaimerField, LM.phoneClaimer.getMapping(claimerKey)));
        properties.add(new ImportProperty(addressClaimerField, LM.addressClaimer.getMapping(claimerKey)));
        properties.add(new ImportProperty(siteClaimerField, LM.siteClaimer.getMapping(claimerKey)));
        properties.add(new ImportProperty(emailClaimerField, LM.emailClaimer.getMapping(claimerKey)));
        properties.add(new ImportProperty(emailFirmClaimerField, LM.emailFirmClaimer.getMapping(claimerKey)));
        properties.add(new ImportProperty(OGRNClaimerField, LM.OGRNClaimer.getMapping(claimerKey)));
        properties.add(new ImportProperty(INNClaimerField, LM.INNClaimer.getMapping(claimerKey)));

        properties.add(new ImportProperty(fileStatementClaimerField, LM.statementClaimer.getMapping(claimerKey)));
        properties.add(new ImportProperty(fileConstituentClaimerField, LM.constituentClaimer.getMapping(claimerKey)));
        properties.add(new ImportProperty(fileExtractClaimerField, LM.extractClaimer.getMapping(claimerKey)));
        properties.add(new ImportProperty(fileNativeSummaryProjectField, LM.fileNativeSummaryProject.getMapping(projectKey)));
        properties.add(new ImportProperty(fileForeignSummaryProjectField, LM.fileForeignSummaryProject.getMapping(projectKey)));
        properties.add(new ImportProperty(fileRoadMapProjectField, LM.fileRoadMapProject.getMapping(projectKey)));
        properties.add(new ImportProperty(fileNativeTechnicalDescriptionProjectField, LM.fileNativeTechnicalDescriptionProject.getMapping(projectKey)));
        properties.add(new ImportProperty(fileForeignTechnicalDescriptionProjectField, LM.fileForeignTechnicalDescriptionProject.getMapping(projectKey)));

        patentKey = new ImportKey(LM.patent, LM.nativeNumberToPatent.getMapping(nativeNumberPatentField));
        propertiesPatent.add(new ImportProperty(nativeTypePatentField, LM.nativeTypePatent.getMapping(patentKey)));
        propertiesPatent.add(new ImportProperty(foreignTypePatentField, LM.foreignTypePatent.getMapping(patentKey)));
        propertiesPatent.add(new ImportProperty(nativeNumberPatentField, LM.nativeNumberPatent.getMapping(patentKey)));
        propertiesPatent.add(new ImportProperty(foreignNumberPatentField, LM.foreignNumberPatent.getMapping(patentKey)));
        propertiesPatent.add(new ImportProperty(datePatentField, LM.priorityDatePatent.getMapping(patentKey)));
        propertiesPatent.add(new ImportProperty(isOwnedPatentField, LM.isOwned.getMapping(patentKey)));
        propertiesPatent.add(new ImportProperty(ownerPatentField, LM.ownerPatent.getMapping(patentKey)));
        propertiesPatent.add(new ImportProperty(isValuatedPatentField, LM.isValuated.getMapping(patentKey)));
        propertiesPatent.add(new ImportProperty(valuatorPatentField, LM.valuatorPatent.getMapping(patentKey)));
        propertiesPatent.add(new ImportProperty(fileIntentionOwnerPatentField, LM.fileIntentionOwnerPatent.getMapping(patentKey)));
        propertiesPatent.add(new ImportProperty(fileActValuationPatentField, LM.fileActValuationPatent.getMapping(patentKey)));
        propertiesPatent.add(new ImportProperty(projectIdField, LM.projectPatent.getMapping(patentKey),
                LM.baseLM.object(LM.project).getMapping(projectKey)));

        ownerTypePatentKey = new ImportKey(LM.ownerType, LM.ownerTypeToSID.getMapping(ownerTypePatentField));
        propertiesPatent.add(new ImportProperty(ownerTypePatentField, LM.ownerTypePatent.getMapping(patentKey),
                LM.baseLM.object(LM.ownerType).getMapping(ownerTypePatentKey)));

        clusterKey = new ImportKey(LM.cluster, LM.nameNativeToCluster.getMapping(nameNativeClusterField));
        propertiesCluster.add(new ImportProperty(inProjectClusterField, LM.inProjectCluster.getMapping(projectKey, clusterKey)));
        propertiesCluster.add(new ImportProperty(nameNativeClusterField, LM.nameNativeCluster.getMapping(clusterKey)));
        propertiesCluster.add(new ImportProperty(nativeSubstantiationProjectClusterField, LM.nativeSubstantiationProjectCluster.getMapping(projectKey, clusterKey)));
        propertiesCluster.add(new ImportProperty(foreignSubstantiationProjectClusterField, LM.foreignSubstantiationProjectCluster.getMapping(projectKey, clusterKey)));

        academicKey = new ImportKey(LM.academic, LM.fullNameToAcademic.getMapping(fullNameAcademicField));
        propertiesAcademic.add(new ImportProperty(fullNameAcademicField, LM.fullNameAcademic.getMapping(academicKey)));
        propertiesAcademic.add(new ImportProperty(institutionAcademicField, LM.institutionAcademic.getMapping(academicKey)));
        propertiesAcademic.add(new ImportProperty(titleAcademicField, LM.titleAcademic.getMapping(academicKey)));
        propertiesAcademic.add(new ImportProperty(fileDocumentConfirmingAcademicField, LM.fileDocumentConfirmingAcademic.getMapping(academicKey)));
        propertiesAcademic.add(new ImportProperty(fileDocumentEmploymentAcademicField, LM.fileDocumentEmploymentAcademic.getMapping(academicKey)));
        propertiesAcademic.add(new ImportProperty(projectIdField, LM.projectAcademic.getMapping(academicKey),
                LM.baseLM.object(LM.project).getMapping(projectKey)));

        nonRussianSpecialistKey = new ImportKey(LM.nonRussianSpecialist, LM.fullNameToNonRussianSpecialist.getMapping(fullNameNonRussianSpecialistField));
        propertiesNonRussianSpecialist.add(new ImportProperty(fullNameNonRussianSpecialistField, LM.fullNameNonRussianSpecialist.getMapping(nonRussianSpecialistKey)));
        propertiesNonRussianSpecialist.add(new ImportProperty(organizationNonRussianSpecialistField, LM.organizationNonRussianSpecialist.getMapping(nonRussianSpecialistKey)));
        propertiesNonRussianSpecialist.add(new ImportProperty(titleNonRussianSpecialistField, LM.titleNonRussianSpecialist.getMapping(nonRussianSpecialistKey)));
        propertiesNonRussianSpecialist.add(new ImportProperty(fileForeignResumeNonRussianSpecialistField, LM.fileForeignResumeNonRussianSpecialist.getMapping(nonRussianSpecialistKey)));
        propertiesNonRussianSpecialist.add(new ImportProperty(fileNativeResumeNonRussianSpecialistField, LM.fileNativeResumeNonRussianSpecialist.getMapping(nonRussianSpecialistKey)));
        propertiesNonRussianSpecialist.add(new ImportProperty(filePassportNonRussianSpecialistField, LM.filePassportNonRussianSpecialist.getMapping(nonRussianSpecialistKey)));
        propertiesNonRussianSpecialist.add(new ImportProperty(fileStatementNonRussianSpecialistField, LM.fileStatementNonRussianSpecialist.getMapping(nonRussianSpecialistKey)));
        propertiesNonRussianSpecialist.add(new ImportProperty(projectIdField, LM.projectNonRussianSpecialist.getMapping(nonRussianSpecialistKey),
                LM.baseLM.object(LM.project).getMapping(projectKey)));
    }

    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
        throw new RuntimeException("no need");
    }

    @Override
    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
        this.session = session;
        toLog = "";

        initFieldsNProperties();

        try {
            String host = getHost();
            Map<String, Timestamp> projects = importProjectsFromXML(host);

            if (!onlyMessage && !fillSids) {
                for (String projectId : projects.keySet()) {
                    URL url = new URL(host + "&show=all&projectId=" + projectId);
                    URLConnection connection = url.openConnection();
                    connection.setDoOutput(false);
                    connection.setDoInput(true);
                    importProject(connection.getInputStream(), projectId, projects.get(projectId));
                }
            }
            String message = "";
            if (projects == null || !projects.isEmpty()) {
                if (!onlyMessage) {
                    message = "Данные были успешно приняты\n";
                    if (!fillSids) {
                        logger.info(toLog);
                    }
                }
                message += toLog;
            } else {
                if (responseContents != null) {
                    message = new String(responseContents);
                    logger.info(toLog);
                } else {
                    message = "Вся информация актуальна";
                }
            }
            actions.add(new MessageClientAction(message, "Импорт", true));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Timestamp> importProjectsFromXML(String host) throws IOException, SQLException {
        OrderedMap<Map<Object, Object>, Map<Object, Object>> result = null;
        List<Element> elementList = null;
        try {
            URL url = new URL(host + "&show=projects&limit=1000");
            URLConnection connection = url.openConnection();

            connection.setDoOutput(false);
            connection.setDoInput(true);

            responseContents = IOUtils.readBytesFromStream(connection.getInputStream());
            SAXBuilder builder = new SAXBuilder();
            Document document = builder.build(new ByteArrayInputStream(responseContents));
            responseContents = null;

            LP isProject = LM.is(LM.project);
            Map<Object, KeyExpr> keys = isProject.getMapKeys();
            Query<Object, Object> query = new Query<Object, Object>(keys);
            query.properties.put("id", LM.sidProject.getExpr(BaseUtils.singleValue(keys)));
            query.properties.put("email", LM.emailClaimerProject.getExpr(BaseUtils.singleValue(keys)));
            query.properties.put("name", LM.nameNative.getExpr(BaseUtils.singleValue(keys)));
            query.properties.put("date", LM.updateDateProject.getExpr(BaseUtils.singleValue(keys)));
            if (fillSids) {
                query.and(isProject.property.getExpr(keys).getWhere());
            } else {
                query.and(LM.sidProject.getExpr(BaseUtils.singleValue(keys)).getWhere());
            }
            result = query.execute(session.sql);

            Element rootNode = document.getRootElement();
            elementList = rootNode.getChildren("project");
        } catch (JDOMParseException e) {
            logger.error(e.getCause() + " : " + new String(responseContents));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (fillSids) {
            fillSids(result, elementList);
            return null;
        }

        return makeImportList(result, elementList);
    }

    public String getHost() throws NoSuchAlgorithmException {
        Calendar calTZ = new GregorianCalendar(TimeZone.getTimeZone("EST"));
        calTZ.setTimeInMillis(new java.util.Date().getTime());
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("EST"));
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.HOUR_OF_DAY, 15);
        cal.set(Calendar.DAY_OF_MONTH, calTZ.get(Calendar.DAY_OF_MONTH) - 3);
        cal.set(Calendar.MONTH, calTZ.get(Calendar.MONTH));
        cal.set(Calendar.YEAR, calTZ.get(Calendar.YEAR));
        String string = cal.getTimeInMillis() / 1000 + "_1q2w3e";

        MessageDigest m = MessageDigest.getInstance("MD5");
        m.update(string.getBytes(), 0, string.length());
        return "http://app.i-gorod.com/xml.php?hash=" + new BigInteger(1, m.digest()).toString(16);
    }

    public Date getUpdateProjectDate(String year, String month, String day, String hour, String minute, String second) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, Integer.valueOf(year));
        calendar.set(Calendar.MONTH, Integer.valueOf(month) - 1);
        calendar.set(Calendar.DAY_OF_MONTH, Integer.valueOf(day));
        calendar.set(Calendar.HOUR_OF_DAY, Integer.valueOf(hour));
        calendar.set(Calendar.MINUTE, Integer.valueOf(minute));
        calendar.set(Calendar.SECOND, Integer.valueOf(second));
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public Map<String, Timestamp> makeImportList(OrderedMap<Map<Object, Object>, Map<Object, Object>> data, List<Element> elementList) {
        Map<String, Timestamp> projectList = new LinkedHashMap<String, Timestamp>();
        if (elementList != null) {
            Timestamp currentProjectDate;
            for (Element element : elementList) {
                String projectId = element.getChildText("projectId");
                currentProjectDate = new Timestamp(getUpdateProjectDate(element.getChildText("yearProject"), element.getChildText("monthProject"),
                        element.getChildText("dayProject"), element.getChildText("hourProject"),
                        element.getChildText("minuteProject"), element.getChildText("secondProject")).getTime());
                String name = element.getChildText("nameNativeProject");
                boolean ignore = false;
                for (Map<Object, Object> project : data.values()) {
                    Timestamp date = (Timestamp) project.get("date");
                    if (project.get("id").toString().trim().equals(projectId)) {
                        if (date == null || currentProjectDate.after(date)) {
                            Object email = project.get("email");
                            toLogString(projectId, email == null ? null : email.toString().trim(), element.getChildText("emailProject"), project.get("name").toString().trim(), name, date, currentProjectDate);
                            projectList.put(projectId, currentProjectDate);
                            ignore = true;
                            break;
                        } else {
                            ignore = true;
                            break;
                        }
                    }
                }
                if (!ignore) {
                    toLogString(projectId, null, element.getChildText("emailProject"), null, name, null, currentProjectDate);
                    projectList.put(projectId, currentProjectDate);
                }
            }
        }
        return projectList;
    }

    public void toLogString(Object projectId, Object oldEmail, Object newEmail, Object oldName, Object newName, Object oldDate, Object newDate) {
        toLog += "\nprojectId: " + projectId + "\n\temail: " + oldEmail + " <- " + newEmail + "\n\tname: " + oldName +
                " <- " + newName + "\n\tdate: " + oldDate + " <- " + newDate;
    }

    public void fillSids(OrderedMap<Map<Object, Object>, Map<Object, Object>> data, List<Element> elements) throws SQLException {
        for (Map<Object, Object> key : data.keySet()) {
            Map<Object, Object> project = data.get(key);
            if (project.get("id") == null) {
                Object email = project.get("email");
                if (email != null && getEmailRepeatings(data, elements, email.toString()) == 1) {
                    String projectId = getProjectId(elements, email.toString());
                    if (projectId != null) {
                        LM.sidProject.execute(projectId, session, session.getDataObject(BaseUtils.singleValue(key), LM.project.getType()));
                        toLog += "\nprojectId: " + projectId + "\n\temail: " + email.toString().trim() + "\n\t" +
                                project.get("name").toString().trim() + " =? " + getProjectName(elements, email.toString());
                    }
                }
            }
        }
    }

    public String getProjectId(List<Element> elements, String email) {
        for (Element element : elements) {
            if (element.getChildText("emailProject").equals(email.trim())) {
                return element.getChildText("projectId");
            }
        }
        return null;
    }

    public String getProjectName(List<Element> elements, String email) {
        for (Element element : elements) {
            if (element.getChildText("emailProject").equals(email.trim())) {
                return element.getChildText("nameNativeProject");
            }
        }
        return null;
    }

    public int getEmailRepeatings(OrderedMap<Map<Object, Object>, Map<Object, Object>> data, List<Element> elements, String email) {
        int repeatings = 0;
        for (Map<Object, Object> project : data.values()) {
            if (project.get("email") != null && project.get("email").toString().equals(email)) {
                repeatings++;
            }
        }
        if (repeatings == 1) {
            repeatings = 0;
            for (Element element : elements) {
                if (element.getChildText("emailProject").equals(email.trim())) {
                    repeatings++;
                }
            }
            return repeatings;
        }
        return 0;
    }

    public void importProject(InputStream inputStream, String projectId, Timestamp currentProjectDate) throws SQLException {
        List<List<Object>> data = new ArrayList<List<Object>>();
        List<List<Object>> dataCluster = new ArrayList<List<Object>>();
        List<List<Object>> dataPatent = new ArrayList<List<Object>>();
        List<List<Object>> dataAcademic = new ArrayList<List<Object>>();
        List<List<Object>> dataNonRussianSpecialist = new ArrayList<List<Object>>();
        List<Object> row;

        SAXBuilder builder = new SAXBuilder();

        try {
            responseContents = IOUtils.readBytesFromStream(inputStream);
            Document document = builder.build(new ByteArrayInputStream(responseContents));
            responseContents = null;
            Element rootNode = document.getRootElement();
            List list = rootNode.getChildren("project");

            for (int i = 0; i < list.size(); i++) {
                Element node = (Element) list.get(i);
                row = new ArrayList<Object>();
                row.add(new java.sql.Date(new Date(Integer.parseInt(node.getChildText("yearProject")) - 1900, Integer.parseInt(node.getChildText("monthProject")) - 1, Integer.parseInt(node.getChildText("dayProject"))).getTime()));
                row.add(node.getChildText("projectId"));
                row.add(node.getChildText("nameNativeProject"));
                row.add(node.getChildText("nameForeignProject"));
                row.add(node.getChildText("nameNativeManagerProject"));
                row.add(node.getChildText("nameNativeGenitiveManagerProject"));
                row.add(node.getChildText("nameNativeDativusManagerProject"));
                row.add(node.getChildText("nameNativeAblateManagerProject"));
                row.add(node.getChildText("nameForeignManagerProject"));
                row.add(node.getChildText("nativeProblemProject"));
                row.add(node.getChildText("foreignProblemProject"));
                row.add(node.getChildText("nativeInnovativeProject"));
                row.add(node.getChildText("foreignInnovativeProject"));
                row.add(node.getChildText("nativeSubstantiationProjectType"));
                row.add(node.getChildText("foreignSubstantiationProjectType"));

                if (Integer.parseInt(BaseUtils.nevl(node.getChildText("isOwnedEquipmentProject"), "0")) == 1)
                    row.add(true);
                else row.add(null);
                if (Integer.parseInt(BaseUtils.nevl(node.getChildText("isAvailableEquipmentProject"), "0")) == 1)
                    row.add(true);
                else row.add(null);
                if (Integer.parseInt(BaseUtils.nevl(node.getChildText("isTransferEquipmentProject"), "0")) == 1)
                    row.add(true);
                else row.add(null);
                row.add(node.getChildText("descriptionTransferEquipmentProject"));
                row.add(node.getChildText("ownerEquipmentProject"));
                if (Integer.parseInt(BaseUtils.nevl(node.getChildText("isPlanningEquipmentProject"), "0")) == 1)
                    row.add(true);
                else row.add(null);
                row.add(node.getChildText("specificationEquipmentProject"));
                if (Integer.parseInt(BaseUtils.nevl(node.getChildText("isSeekEquipmentProject"), "0")) == 1)
                    row.add(true);
                else row.add(null);
                row.add(node.getChildText("descriptionEquipmentProject"));
                if (Integer.parseInt(BaseUtils.nevl(node.getChildText("isOtherEquipmentProject"), "0")) == 1)
                    row.add(true);
                else row.add(null);
                row.add(node.getChildText("commentEquipmentProject"));

                if (Integer.parseInt(BaseUtils.nevl(node.getChildText("isReturnInvestmentsProject"), "0")) == 1)
                    row.add(true);
                else row.add(null);
                row.add(node.getChildText("nameReturnInvestorProject"));
                row.add(node.getChildText("amountReturnFundsProject"));

                if (Integer.parseInt(BaseUtils.nevl(node.getChildText("isNonReturnInvestmentsProject"), "0")) == 1)
                    row.add(true);
                else row.add(null);
                row.add(node.getChildText("nameNonReturnInvestorProject"));
                row.add(("amountNonReturnFundsProject"));
                row.add(node.getChildText("commentOtherNonReturnInvestmentsProject"));

                if (Integer.parseInt(BaseUtils.nevl(node.getChildText("isCapitalInvestmentProject"), "0")) == 1)
                    row.add(true);
                else row.add(null);
                if (Integer.parseInt(BaseUtils.nevl(node.getChildText("isPropertyInvestmentProject"), "0")) == 1)
                    row.add(true);
                else row.add(null);
                if (Integer.parseInt(BaseUtils.nevl(node.getChildText("isGrantsProject"), "0")) == 1)
                    row.add(true);
                else row.add(null);
                if (Integer.parseInt(BaseUtils.nevl(node.getChildText("isOtherNonReturnInvestmentsProject"), "0")) == 1)
                    row.add(true);
                else row.add(null);

                if (Integer.parseInt(BaseUtils.nevl(node.getChildText("isOwnFundsProject"), "0")) == 1)
                    row.add(true);
                else row.add(null);
                row.add(node.getChildText("amountOwnFundsProject"));
                if (Integer.parseInt(BaseUtils.nevl(node.getChildText("isPlanningSearchSourceProject"), "0")) == 1)
                    row.add(true);
                else row.add(null);
                row.add(node.getChildText("amountFundsProject"));
                if (Integer.parseInt(BaseUtils.nevl(node.getChildText("isOtherSoursesProject"), "0")) == 1)
                    row.add(true);
                else row.add(null);
                row.add(node.getChildText("commentOtherSoursesProject"));
                row.add(currentProjectDate);

                row.add(node.getChildText("typeProject"));
                row.add(node.getChildText("actionProject"));
                row.add(node.getChildText("nameNativeClaimer"));
                row.add(node.getChildText("nameForeignClaimer"));

                row.add(node.getChildText("firmNameNativeClaimer"));
                row.add(node.getChildText("firmNameForeignClaimer"));
                row.add(node.getChildText("phoneClaimer"));
                row.add(node.getChildText("addressClaimer"));
                row.add(node.getChildText("siteClaimer"));
                row.add(node.getChildText("emailProject"));
                row.add(node.getChildText("emailClaimer"));
                row.add(node.getChildText("OGRNClaimer"));
                row.add(node.getChildText("INNClaimer"));

                byte[] fileBytes = Base64.decodeBase64(BaseUtils.nevl(node.getChildText("fileStatementClaimer"), null));
                row.add(fileBytes);
                fileBytes = Base64.decodeBase64(BaseUtils.nevl(node.getChildText("fileConstituentClaimer"), null));
                row.add(fileBytes);
                fileBytes = Base64.decodeBase64(BaseUtils.nevl(node.getChildText("fileExtractClaimer"), null));
                row.add(fileBytes);
                fileBytes = Base64.decodeBase64(BaseUtils.nevl(node.getChildText("fileNativeSummaryProject"), null));
                row.add(fileBytes);
                fileBytes = Base64.decodeBase64(BaseUtils.nevl(node.getChildText("fileForeignSummaryProject"), null));
                row.add(fileBytes);
                fileBytes = Base64.decodeBase64(BaseUtils.nevl(node.getChildText("fileRoadMapProject"), null));
                row.add(fileBytes);
                fileBytes = Base64.decodeBase64(BaseUtils.nevl(node.getChildText("fileNativeTechnicalDescriptionProject"), null));
                row.add(fileBytes);
                fileBytes = Base64.decodeBase64(BaseUtils.nevl(node.getChildText("fileForeignTechnicalDescriptionProject"), null));
                row.add(fileBytes);

                data.add(row);

                List listCluster = node.getChildren("cluster");
                for (int z = 0; z < listCluster.size(); z++) {
                    Element nodeCluster = (Element) listCluster.get(z);
                    List<Object> rowCluster = new ArrayList<Object>();

                    rowCluster.add(true);

                    rowCluster.add(nodeCluster.getChildText("nameNativeCluster"));
                    rowCluster.add(node.getChildText("projectId"));
                    rowCluster.add(nodeCluster.getChildText("nativeSubstantiationClusterProject"));
                    rowCluster.add(nodeCluster.getChildText("foreignSubstantiationClusterProject"));
                    dataCluster.add(rowCluster);
                }

                int j;

                List listPatent = node.getChildren("patent");
                for (j = 0; j < listPatent.size(); j++) {
                    Element nodePatent = (Element) listPatent.get(j);
                    List<Object> rowPatent = new ArrayList<Object>();

                    rowPatent.add(node.getChildText("projectId"));
                    rowPatent.add(nodePatent.getChildText("nativeTypePatent"));
                    rowPatent.add(nodePatent.getChildText("foreignTypePatent"));
                    rowPatent.add(nodePatent.getChildText("nativeNumberPatent"));
                    rowPatent.add(nodePatent.getChildText("foreignNumberPatent"));
                    try {
                        rowPatent.add(new java.sql.Date(new Date(Integer.parseInt(nodePatent.getChildText("yearPatent")) - 1900, Integer.parseInt(nodePatent.getChildText("monthPatent")), Integer.parseInt(nodePatent.getChildText("dayPatent")) - 1).getTime()));
                    } catch (NumberFormatException e) {
                        rowPatent.add(null);
                    }
                    if (Integer.parseInt(BaseUtils.nevl(nodePatent.getChildText("isOwnedPatent"), "0")) == 1)
                        rowPatent.add(true);
                    else rowPatent.add(null);
                    rowPatent.add(nodePatent.getChildText("ownerPatent"));
                    rowPatent.add(nodePatent.getChildText("ownerTypePatent"));
                    if (Integer.parseInt(BaseUtils.nevl(nodePatent.getChildText("isValuatedPatent"), "0")) == 1)
                        rowPatent.add(true);
                    else rowPatent.add(null);
                    rowPatent.add(nodePatent.getChildText("valuatorPatent"));
                    fileBytes = Base64.decodeBase64(BaseUtils.nevl(nodePatent.getChildText("fileIntentionOwnerPatent"), null));
                    rowPatent.add(fileBytes);
                    fileBytes = Base64.decodeBase64(BaseUtils.nevl(nodePatent.getChildText("fileActValuationPatent"), null));
                    rowPatent.add(fileBytes);

                    dataPatent.add(rowPatent);
                }

                List listAcademic = node.getChildren("academic");
                for (j = 0; j < listAcademic.size(); j++) {
                    Element nodeAcademic = (Element) listAcademic.get(j);
                    List<Object> rowAcademic = new ArrayList<Object>();
                    rowAcademic.add(node.getChildText("projectId"));
                    rowAcademic.add(nodeAcademic.getChildText("fullNameAcademic"));
                    rowAcademic.add(nodeAcademic.getChildText("institutionAcademic"));
                    rowAcademic.add(nodeAcademic.getChildText("titleAcademic"));
                    fileBytes = Base64.decodeBase64(BaseUtils.nevl(nodeAcademic.getChildText("fileDocumentConfirmingAcademic"), null));
                    rowAcademic.add(fileBytes);
                    fileBytes = Base64.decodeBase64(BaseUtils.nevl(nodeAcademic.getChildText("fileDocumentEmploymentAcademic"), null));
                    rowAcademic.add(fileBytes);
                    dataAcademic.add(rowAcademic);
                }

                List listNonRussianSpecialist = node.getChildren("nonRussianSpecialist");
                for (j = 0; j < listNonRussianSpecialist.size(); j++) {
                    Element nodeNonRussianSpecialist = (Element) listNonRussianSpecialist.get(j);
                    List<Object> rowNonRussianSpecialist = new ArrayList<Object>();
                    rowNonRussianSpecialist.add(node.getChildText("projectId"));
                    rowNonRussianSpecialist.add(nodeNonRussianSpecialist.getChildText("fullNameNonRussianSpecialist"));
                    rowNonRussianSpecialist.add(nodeNonRussianSpecialist.getChildText("organizationNonRussianSpecialist"));
                    rowNonRussianSpecialist.add(nodeNonRussianSpecialist.getChildText("titleNonRussianSpecialist"));
                    rowNonRussianSpecialist.add(fileBytes);
                    fileBytes = Base64.decodeBase64(BaseUtils.nevl(nodeNonRussianSpecialist.getChildText("fileNativeResumeNonRussianSpecialist"), null));
                    rowNonRussianSpecialist.add(fileBytes);
                    fileBytes = Base64.decodeBase64(BaseUtils.nevl(nodeNonRussianSpecialist.getChildText("filePassportNonRussianSpecialist"), null));
                    rowNonRussianSpecialist.add(fileBytes);
                    fileBytes = Base64.decodeBase64(BaseUtils.nevl(nodeNonRussianSpecialist.getChildText("fileForeignResumeNonRussianSpecialist"), null));
                    fileBytes = Base64.decodeBase64(BaseUtils.nevl(nodeNonRussianSpecialist.getChildText("fileStatementNonRussianSpecialist"), null));
                    rowNonRussianSpecialist.add(fileBytes);
                    dataNonRussianSpecialist.add(rowNonRussianSpecialist);
                }
            }
        } catch (JDOMParseException e) {
            toLog += e.getCause() + " : projectId=" + projectId + " : " + new String(responseContents);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        List<ImportField> fields = BaseUtils.toList(
                dateProjectField, projectIdField, nameNativeProjectField, nameForeignProjectField,
                nameNativeManagerProjectField, nameNativeGenitiveManagerProjectField, nameNativeDativusManagerProjectField, nameNativeAblateManagerProjectField,
                nameForeignManagerProjectField,
                nativeProblemProjectField, foreignProblemProjectField,
                nativeInnovativeProjectField, foreignInnovativeProjectField,
                nativeSubstantiationProjectTypeField, foreignSubstantiationProjectTypeField,
                isOwnedEquipmentProjectField, isAvailableEquipmentProjectField,
                isTransferEquipmentProjectField, descriptionTransferEquipmentProjectField, ownerEquipmentProjectField,
                isPlanningEquipmentProjectField, specificationEquipmentProjectField,
                isSeekEquipmentProjectField, descriptionEquipmentProjectField,
                isOtherEquipmentProjectField, commentEquipmentProjectField,
                isReturnInvestmentsProjectField, nameReturnInvestorProjectField, amountReturnFundsProjectField,
                isNonReturnInvestmentsProjectField, nameNonReturnInvestorProjectField, amountNonReturnFundsProjectField, commentOtherNonReturnInvestmentsProjectField,
                isCapitalInvestmentProjectField, isPropertyInvestmentProjectField, isGrantsProjectField, isOtherNonReturnInvestmentsProjectField,
                isOwnFundsProjectField, amountOwnFundsProjectField,
                isPlanningSearchSourceProjectField, amountFundsProjectField,
                isOtherSoursesProjectField, commentOtherSoursesProjectField, updateDateProjectField,
                projectTypeProjectField, projectActionProjectField,
                nameNativeClaimerField, nameForeignClaimerField,
                firmNameNativeClaimerField, firmNameForeignClaimerField, phoneClaimerField, addressClaimerField,
                siteClaimerField, emailClaimerField, emailFirmClaimerField, OGRNClaimerField, INNClaimerField,
                fileStatementClaimerField, fileConstituentClaimerField, fileExtractClaimerField,
                fileNativeSummaryProjectField, fileForeignSummaryProjectField,
                fileRoadMapProjectField,
                fileNativeTechnicalDescriptionProjectField, fileForeignTechnicalDescriptionProjectField
        );
        ImportTable table = new ImportTable(fields, data);

        ImportKey<?>[] keysArray;
        keysArray = new ImportKey<?>[]{projectKey, projectTypeProjectKey, projectActionProjectKey, claimerKey};
        new IntegrationService(session, table, Arrays.asList(keysArray), properties).synchronize(true, true, false);

        List<ImportField> fieldsCurrentCluster = BaseUtils.toList(

                inProjectClusterField, nameNativeClusterField, projectIdField, nativeSubstantiationProjectClusterField, foreignSubstantiationProjectClusterField);
        table = new ImportTable(fieldsCurrentCluster, dataCluster);
        keysArray = new ImportKey<?>[]{clusterKey, projectKey};
        new IntegrationService(session, table, Arrays.asList(keysArray), propertiesCluster).synchronize(true, true, false);

        List<ImportField> fieldsPatent = BaseUtils.toList(
                projectIdField,
                nativeTypePatentField, foreignTypePatentField, nativeNumberPatentField, foreignNumberPatentField,
                datePatentField, isOwnedPatentField, ownerPatentField, ownerTypePatentField,
                isValuatedPatentField, valuatorPatentField,
                fileIntentionOwnerPatentField, fileActValuationPatentField
        );
        table = new ImportTable(fieldsPatent, dataPatent);
        keysArray = new ImportKey<?>[]{projectKey, patentKey, ownerTypePatentKey};
        new IntegrationService(session, table, Arrays.asList(keysArray), propertiesPatent).synchronize(true, true, false);

        List<ImportField> fieldsAcademic = BaseUtils.toList(
                projectIdField,
                fullNameAcademicField, institutionAcademicField, titleAcademicField,
                fileDocumentConfirmingAcademicField, fileDocumentEmploymentAcademicField
        );
        table = new ImportTable(fieldsAcademic, dataAcademic);
        keysArray = new ImportKey<?>[]{projectKey, academicKey};
        new IntegrationService(session, table, Arrays.asList(keysArray), propertiesAcademic).synchronize(true, true, false);

        List<ImportField> fieldsNonRussianSpecialist = BaseUtils.toList(
                projectIdField,
                fullNameNonRussianSpecialistField, organizationNonRussianSpecialistField, titleNonRussianSpecialistField
                , fileForeignResumeNonRussianSpecialistField, fileNativeResumeNonRussianSpecialistField,
                filePassportNonRussianSpecialistField, fileStatementNonRussianSpecialistField
        );
        table = new ImportTable(fieldsNonRussianSpecialist, dataNonRussianSpecialist);
        keysArray = new ImportKey<?>[]{projectKey, nonRussianSpecialistKey};
        new IntegrationService(session, table, Arrays.asList(keysArray), propertiesNonRussianSpecialist).synchronize(true, true, false);
    }
}
