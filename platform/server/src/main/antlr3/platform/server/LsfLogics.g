grammar LsfLogics;

@header {
	package platform.server;

	import platform.interop.ClassViewType;
	import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
	import platform.server.data.Union;
	import platform.server.data.expr.query.OrderType;
	import platform.server.form.view.*;
	import platform.server.logics.scripted.ScriptedFormView;
	import platform.server.logics.scripted.ScriptedFormView.InsertPosition;
	import platform.server.logics.scripted.ScriptingErrorLog;
	import platform.server.logics.scripted.ScriptingFormEntity;
	import platform.server.logics.scripted.ScriptingLogicsModule;
	import platform.server.logics.linear.LP;
	import platform.server.logics.property.PropertyFollows;

	import java.awt.*;
	import java.util.ArrayList;
	import java.util.Collections;
	import java.util.List;
	import java.util.Stack;

	import static platform.interop.form.layout.SingleSimplexConstraint.*;
}

@lexer::header { 
	package platform.server; 
	import platform.server.logics.scripted.ScriptingLogicsModule;
}

@lexer::members {
	public ScriptingLogicsModule self;
	public ScriptingLogicsModule.State parseState;
	
	@Override
	public void emitErrorMessage(String msg) {
		if (parseState == ScriptingLogicsModule.State.GROUP) { 
			self.getErrLog().write(msg + "\n");
		}
	}
	
	@Override
	public String getErrorMessage(RecognitionException e, String[] tokenNames) {
		return self.getErrLog().getErrorMessage(this, super.getErrorMessage(e, tokenNames), e);
	} 	
	
	@Override
	public void displayRecognitionError(String[] tokenNames, RecognitionException e) {
		self.getErrLog().displayRecognitionError(this, "error", tokenNames, e);
	}
}

@members {
	public ScriptingLogicsModule self;
	public ScriptingLogicsModule.State parseState;
	

	public boolean inParseState(ScriptingLogicsModule.State parseState) {
		return this.parseState == parseState;
	}

	public boolean inGroupParseState() {
		return inParseState(ScriptingLogicsModule.State.GROUP);
	}

	public boolean inClassParseState() {
		return inParseState(ScriptingLogicsModule.State.CLASS);
	}

	public boolean inPropParseState() {
		return inParseState(ScriptingLogicsModule.State.PROP);
	}

	public boolean inNavigatorParseState() {
		return inParseState(ScriptingLogicsModule.State.NAVIGATOR);
	}

	public void setObjectProperty(Object propertyReceiver, String propertyName, Object propertyValue) throws ScriptingErrorLog.SemanticErrorException {
		if (inNavigatorParseState()) {
			$designStatement::design.setObjectProperty(propertyReceiver, propertyName, propertyValue);
		}
    }

	@Override
	public void emitErrorMessage(String msg) {
		if (parseState == ScriptingLogicsModule.State.GROUP) { 
			self.getErrLog().write(msg + "\n");
		}
	}	

	@Override
	public String getErrorMessage(RecognitionException e, String[] tokenNames) {
		return self.getErrLog().getErrorMessage(this, super.getErrorMessage(e, tokenNames), e);
	} 	

	@Override
	public void displayRecognitionError(String[] tokenNames, RecognitionException e) {
		self.getErrLog().displayRecognitionError(this, "error", tokenNames, e);
	}
}

@rulecatch {
	catch(RecognitionException re) {
		if (re instanceof ScriptingErrorLog.SemanticErrorException) {
			throw re;
		} else {
			reportError(re);
			recover(input,re);
		}
	}
}

script	
	:	importDirective* statement*
	;


importDirective
@after {
	if (parseState == ScriptingLogicsModule.State.GROUP) {
		self.addImportedModule($moduleName.text);
	}
}
	:	'IMPORT' moduleName=ID ';'
	;


statement
	:   (   classStatement
	    | 	groupStatement
	    | 	propertyStatement
	    | 	constraintStatement
	    |	followsStatement
	    |	writeOnChangeStatement
	    | 	tableStatement
	    | 	indexStatement
	    | 	formStatement
	    ) ';'
    | designStatement
	;


////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// CLASS STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////


classStatement 
@init {
	List<String> classParents = new ArrayList<String>();
	String name = null; 
	String captionStr = null;
	boolean isAbstract = false;
	boolean isStatic = false;
	List<String> instanceNames = new ArrayList<String>();
	List<String> instanceCaptions = new ArrayList<String>();
}
@after {
	if (parseState == ScriptingLogicsModule.State.CLASS) {
		self.addScriptedClass(name, captionStr, isAbstract, isStatic, instanceNames, instanceCaptions, classParents);
	}
}
	:	'CLASS' ('ABSTRACT' {isAbstract = true;} | 'STATIC' {isStatic = true;})?
		nameCaption=simpleNameWithCaption { name = $nameCaption.name; captionStr = $nameCaption.caption; }
		(
		'{'
			firstInstData=simpleNameWithCaption { instanceNames.add($firstInstData.name); instanceCaptions.add($firstInstData.caption); }
			(',' nextInstData=simpleNameWithCaption { instanceNames.add($nextInstData.name); instanceCaptions.add($nextInstData.caption); })*
		'}'
		)?
		(':'
		parentList=nonEmptyCompoundIdList { classParents = $parentList.ids; })?
	;	  


////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// GROUP STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////
groupStatement
@init {
	String parent = null;
	String name = null;
	String captionStr = null;
}
@after {
	if (parseState == ScriptingLogicsModule.State.GROUP) {
		self.addScriptedGroup(name, captionStr, parent);
	}
}
	:	'GROUP' groupNameCaption=simpleNameWithCaption { name = $groupNameCaption.name; captionStr = $groupNameCaption.caption; }
		(':' parentName=compoundID { parent = $parentName.text; })?
	;


////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////// FORM STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

formStatement
@init {
	ScriptingFormEntity form;
}
@after {
	if (parseState == ScriptingLogicsModule.State.NAVIGATOR) {
		self.addScriptedForm(form);
	}
}
	:	declaration=formDeclaration { form = $declaration.form; }
		(	'OBJECTS' list=formGroupObjectsList[form] 
		|	'FILTERS' list=formFiltersList[form]
		| 	formPropertiesList[form]
		)*
	;


formDeclaration returns [ScriptingFormEntity form]
@init {
	String name = null;
	String caption = null;
}
@after {
	if (parseState == ScriptingLogicsModule.State.NAVIGATOR) {
		$form = self.createScriptedForm(name, caption);
	}
}
	:	'FORM' 
		formNameCaption=simpleNameWithCaption { name = $formNameCaption.name; caption = $formNameCaption.caption; }
	;


formGroupObjectsList[ScriptingFormEntity form]  // needs refactoring
@init {
	List<List<String>> names = new ArrayList<List<String>>();
	List<List<String>> classNames = new ArrayList<List<String>>(); 
	List<ClassViewType> groupViewType = new ArrayList<ClassViewType>();
	List<Boolean> isInitType = new ArrayList<Boolean>();
}
@after {
	if (parseState == ScriptingLogicsModule.State.NAVIGATOR) {
		$form.addScriptedGroupObjects(names, classNames, groupViewType, isInitType);
	}
}
	:	groupElement=formGroupObjectDeclaration { names.add($groupElement.objectNames); classNames.add($groupElement.classIds); 
												  groupViewType.add($groupElement.type); isInitType.add($groupElement.isInitType); } 
		(',' groupElement=formGroupObjectDeclaration { names.add($groupElement.objectNames); classNames.add($groupElement.classIds); 
													   groupViewType.add($groupElement.type); isInitType.add($groupElement.isInitType); })*
	;


formGroupObjectDeclaration returns [List<String> objectNames, List<String> classIds, ClassViewType type, boolean isInitType]
@init {
	$objectNames = new ArrayList<String>();
	$classIds = new ArrayList<String>();
}
	:	(decl=formSingleGroupObjectDeclaration { $objectNames.add($decl.name); $classIds.add($decl.className); }
	|	('(' 
		objDecl=formObjectDeclaration { $objectNames.add($objDecl.name); $classIds.add($objDecl.className); }	
		(',' objDecl=formObjectDeclaration { $objectNames.add($objDecl.name); $classIds.add($objDecl.className); })+	
		')'))
		(viewType=formGroupObjectViewType { $type = $viewType.type; $isInitType = $viewType.isInitType; })?
	; 


formGroupObjectViewType returns [ClassViewType type, boolean isInitType]
	: 	('INIT' {$isInitType = true;} | 'FIXED' {$isInitType = false;})
	  	('PANEL' {$type = ClassViewType.PANEL;} | 'HIDE' {$type = ClassViewType.HIDE;} | 'GRID' {$type = ClassViewType.GRID;})
	;


formSingleGroupObjectDeclaration returns [String name, String className] 
	:	foDecl=formObjectDeclaration { $name = $foDecl.name; $className = $foDecl.className; }
	;


formObjectDeclaration returns [String name, String className] 
	:	(objectName=ID { $name = $objectName.text; } '=')?	
		id=classId { $className = $id.text; }
	; 
	
	
formPropertiesList[ScriptingFormEntity form]
@init {
	List<String> properties = new ArrayList<String>();
	List<List<String>> mapping = new ArrayList<List<String>>();
}
@after {
	if (parseState == ScriptingLogicsModule.State.NAVIGATOR) {
		$form.addScriptedPropertyDraws(properties, mapping);
	}
}
	: 'PROPERTIES' '(' objects=idList ')' list=formPropertiesNamesList
		{
			properties = $list.properties;
			mapping = Collections.nCopies(properties.size(), $objects.ids);
		}
	| 'PROPERTIES' mappedList=formMappedPropertiesList
		{
			properties = $mappedList.properties;
			mapping = $mappedList.mapping;
		}
	;	


formMappedPropertiesList returns [List<String> properties, List<List<String>> mapping]
@init {
	$properties = new ArrayList<String>();
	$mapping = new ArrayList<List<String>>();
}
	:	mappedProp=formMappedProperty { $properties.add($mappedProp.name); $mapping.add($mappedProp.mapping); }
		(',' mappedProp=formMappedProperty { $properties.add($mappedProp.name); $mapping.add($mappedProp.mapping); })*
	;


formMappedProperty returns [String name, List<String> mapping]
	: pname=formPropertyName { $name = $pname.text; }
	'('
		objects=idList { $mapping = $objects.ids; }
	')'
	;


formPropertiesNamesList returns [List<String> properties]
@init {
	$properties = new ArrayList<String>();
}
	:	pname=formPropertyName { properties.add($pname.text); }
		(
			',' pname=formPropertyName { properties.add($pname.text); }
		)*
	;


formPropertyName 
	: compoundID | ('OBJVALUE') | ('SELECTION') | ('ADDOBJ')
	;


formFiltersList[ScriptingFormEntity form] 
@init {
	List<String> propertyNames = new ArrayList<String>();
	List<List<String>> propertyMappings = new ArrayList<List<String>>();
}
@after {
	if (parseState == ScriptingLogicsModule.State.NAVIGATOR) {
		$form.addScriptedFilters(propertyNames, propertyMappings);
	}
}
	: decl=formFilterDeclaration { propertyNames.add($decl.name); propertyMappings.add($decl.mapping);}
	  (',' decl=formFilterDeclaration { propertyNames.add($decl.name); propertyMappings.add($decl.mapping);})*
	;

	
formFilterDeclaration returns [String name, List<String> mapping] 
	: 'NOT' 'NULL' propDecl=formMappedProperty { $name = $propDecl.name; $mapping = $propDecl.mapping; }
	;	

////////////////////////////////////////////////////////////////////////////////
//////////////////////////////// PROPERTY STATEMENT ////////////////////////////
////////////////////////////////////////////////////////////////////////////////

propertyStatement
@init {
	LP<?> property = null;
	boolean isData = false;
	List<String> context = new ArrayList<String>();
	boolean dynamic = true;
}
	:	declaration=propertyDeclaration { if ($declaration.paramNames != null) { context = $declaration.paramNames; dynamic = false; }}
		'=' 
		(	def=contextIndependentPD[false] { property = $def.property; isData = $def.isData; }  
		|	expr=propertyExpression[context, dynamic] { property = $expr.property; }
		)
		settings=commonPropertySettings[property, $declaration.name, $declaration.caption, context, isData]
	;


propertyDeclaration returns [String name, String caption, List<String> paramNames]
	:	propNameCaption=simpleNameWithCaption { $name = $propNameCaption.name; $caption = $propNameCaption.caption; }
		('(' paramList=idList ')' { $paramNames = $paramList.ids; })? 
	;


propertyExpression[List<String> context, boolean dynamic] returns [LP property, List<Integer> usedParams]
	:	pe=andPE[context, dynamic] { $property = $pe.property; $usedParams = $pe.usedParams; }
	;


andPE[List<String> context, boolean dynamic] returns [LP property, List<Integer> usedParams]
@init {
	List<LP<?>> props = new ArrayList<LP<?>>();
	List<List<Integer>> allUsedParams = new ArrayList<List<Integer>>();
	List<Boolean> nots = new ArrayList<Boolean>();
}
@after {
	if (parseState == ScriptingLogicsModule.State.PROP) {
		ScriptingLogicsModule.LPWithParams result = self.addScriptedAndProp(nots, props, allUsedParams);				
		$property = result.property;
		$usedParams = result.usedParams;
	}
}
	:	firstExpr=equalityPE[context, dynamic] { props.add($firstExpr.property); allUsedParams.add($firstExpr.usedParams); }
		((('AND') | ('IF')) { nots.add(false); }
		('NOT' { nots.set(nots.size()-1, true); })?
		nextExpr=equalityPE[context, dynamic] { props.add($nextExpr.property); allUsedParams.add($nextExpr.usedParams); })*
	;
		

equalityPE[List<String> context, boolean dynamic] returns [LP property, List<Integer> usedParams]
@init {
	LP<?> leftProp = null;
	LP<?> rightProp = null;
	List<Integer> lUsedParams = null, rUsedParams = null;
	String op = null;
}
@after {
	if (parseState == ScriptingLogicsModule.State.PROP && op != null) {
		ScriptingLogicsModule.LPWithParams result =	
			self.addScriptedEqualityProp(op, leftProp, lUsedParams, rightProp, rUsedParams);
		$property = result.property;
		$usedParams = result.usedParams;
	} else {
		$property = leftProp;
		$usedParams = lUsedParams;
	}
}
	:	lhs=relationalPE[context, dynamic] { leftProp = $lhs.property; lUsedParams = $lhs.usedParams; }
		(operand=EQ_OPERAND { op = $operand.text; }
		rhs=relationalPE[context, dynamic] { rightProp = $rhs.property; rUsedParams = $rhs.usedParams; })?
	;


relationalPE[List<String> context, boolean dynamic] returns [LP property, List<Integer> usedParams]
@init {
	LP<?> leftProp = null;
	LP<?> rightProp = null;
	LP<?> mainProp = null;
	List<Integer> lUsedParams = null, rUsedParams = null;
	String op = null;
}
@after {
	if (parseState == ScriptingLogicsModule.State.PROP)
	{
		if (op != null) {
			ScriptingLogicsModule.LPWithParams result =
				self.addScriptedRelationalProp(op, leftProp, lUsedParams, rightProp, rUsedParams);
			$property = result.property;
			$usedParams = result.usedParams;
		} else if (mainProp != null) {
			$property = self.addScriptedTypeExprProp(mainProp, leftProp, lUsedParams);
			$usedParams = lUsedParams;
		} else {
			$property = leftProp;
			$usedParams = lUsedParams;
		}
	}	
}
	:	lhs=additivePE[context, dynamic] { leftProp = $lhs.property; lUsedParams = $lhs.usedParams; }
		(
			(operand=REL_OPERAND { op = $operand.text; }
			rhs=additivePE[context, dynamic] { rightProp = $rhs.property; rUsedParams = $rhs.usedParams; }) 
		|	def=typePropertyDefinition { mainProp = $def.property; }
		)?
	;


additivePE[List<String> context, boolean dynamic] returns [LP property, List<Integer> usedParams]
@init {
	List<LP<?>> props = new ArrayList<LP<?>>();
	List<List<Integer>> allUsedParams = new ArrayList<List<Integer>>();
	List<String> ops = new ArrayList<String>();
}
@after {
	if (parseState == ScriptingLogicsModule.State.PROP) {
		ScriptingLogicsModule.LPWithParams result = self.addScriptedAdditiveProp(ops, props, allUsedParams);				
		$property = result.property;
		$usedParams = result.usedParams;
	}
}
	:	firstExpr=multiplicativePE[context, dynamic] { props.add($firstExpr.property); allUsedParams.add($firstExpr.usedParams); }
		( (operand=PLUS | operand=MINUS) { ops.add($operand.text); }
		nextExpr=multiplicativePE[context, dynamic] { props.add($nextExpr.property); allUsedParams.add($nextExpr.usedParams); })*
	;
		
	
multiplicativePE[List<String> context, boolean dynamic] returns [LP property, List<Integer> usedParams]
@init {
	List<LP<?>> props = new ArrayList<LP<?>>();
	List<List<Integer>> allUsedParams = new ArrayList<List<Integer>>();
	List<String> ops = new ArrayList<String>();
}
@after {
	if (parseState == ScriptingLogicsModule.State.PROP) {
		ScriptingLogicsModule.LPWithParams result = self.addScriptedMultiplicativeProp(ops, props, allUsedParams);				
		$property = result.property;
		$usedParams = result.usedParams;
	}
}
	:	firstExpr=simplePE[context, dynamic] { props.add($firstExpr.property); allUsedParams.add($firstExpr.usedParams); }
		(operand=MULT_OPERAND { ops.add($operand.text); }
		nextExpr=simplePE[context, dynamic] { props.add($nextExpr.property); allUsedParams.add($nextExpr.usedParams); })*
	;
	
		 
	 
simplePE[List<String> context, boolean dynamic] returns [LP property, List<Integer> usedParams]
	:	'(' expr=propertyExpression[context, dynamic] ')' { $property = $expr.property; $usedParams = $expr.usedParams; } 
	|	primitive=expressionPrimitive[context, dynamic] { $property = $primitive.property; $usedParams = $primitive.usedParams; } 
	|	uexpr=unaryMinusPE[context, dynamic] { $property = $uexpr.property; $usedParams = $uexpr.usedParams; }
	;

	
unaryMinusPE[List<String> context, boolean dynamic] returns [LP property, List<Integer> usedParams] 	
@after {
	if (parseState == ScriptingLogicsModule.State.PROP) { 
		$property = self.addScriptedUnaryMinusProp($property, $usedParams);
	}
}
	: MINUS expr=simplePE[context, dynamic] { $property = $expr.property; $usedParams = $expr.usedParams; }
	;		 
	

expressionPrimitive[List<String> context, boolean dynamic] returns [LP property, List<Integer> usedParams]
	:	(paramName=parameter {
			if (parseState == ScriptingLogicsModule.State.PROP)
				$usedParams = Collections.singletonList(self.getParamIndex($paramName.text, $context, $dynamic));
		 })
	|	(expr=contextDependentPD[context, dynamic] { $property = $expr.property; $usedParams = $expr.usedParams; })
	;

propertyDefinition[List<String> context, boolean dynamic] returns [LP property, List<Integer> usedParams]
	:	propertyExpr=contextDependentPD[context, dynamic] { $property = $propertyExpr.property; $usedParams = $propertyExpr.usedParams; } 
	|	propertyExprI=contextIndependentPD[true] { $property = $propertyExprI.property; $usedParams = new ArrayList<Integer>(); }
	;

contextDependentPD[List<String> context, boolean dynamic] returns [LP property, List<Integer> usedParams]
	:	joinDef=joinPropertyDefinition[context, dynamic] { $property = $joinDef.property; $usedParams = $joinDef.usedParams; } 
	|	unionDef=unionPropertyDefinition[context, dynamic] { $property = $unionDef.property; $usedParams = $unionDef.usedParams; } 
	|	partDef=partitionPropertyDefinition[context, dynamic] { $property = $partDef.property; $usedParams = $partDef.usedParams; }
	|	constDef=literal { $property = $constDef.property; $usedParams = new ArrayList<Integer>(); }
	;

contextIndependentPD[boolean innerPD] returns [LP property, boolean isData]
	: 	dataDef=dataPropertyDefinition[innerPD] { $property = $dataDef.property; $isData = true; } 
	|	formulaProp=formulaPropertyDefinition { $property = $formulaProp.property; $isData = false; }
	|	groupDef=groupPropertyDefinition { $property = $groupDef.property; $isData = false; } 
	|	typeDef=typePropertyDefinition { $property = $typeDef.property; $isData = false; }
	;

joinPropertyDefinition[List<String> context, boolean dynamic] returns [LP property, List<Integer> usedParams]
@init {
	List<LP<?>> paramProps;
	List<List<Integer>> usedSubParams;
	LP mainProp = null;
}
@after {
	if (parseState == ScriptingLogicsModule.State.PROP) {
		ScriptingLogicsModule.LPWithParams result = self.addScriptedJProp(mainProp, paramProps, usedSubParams);
		$property = result.property;
		$usedParams = result.usedParams;
	}
}
	:	mainPropObj=propertyObject { mainProp = $mainPropObj.property; }
		'('
		exprList=propertyExpressionList[context, dynamic] { paramProps = $exprList.props; usedSubParams = $exprList.usedParams; }	
		')'
	;




groupPropertyDefinition returns [LP property]
@init {
	List<LP<?>> paramProps = new ArrayList<LP<?>>();
	List<List<Integer>> usedParams = new ArrayList<List<Integer>>();
	boolean isSGProp = true;
	List<String> groupContext = new ArrayList<String>();
}
@after {
	if (parseState == ScriptingLogicsModule.State.PROP) {
		$property = self.addScriptedGProp(isSGProp, paramProps, usedParams);
	}
}
	:	'GROUP' (('SUM') { isSGProp = true; } | ('MAX') { isSGProp = false; })
		prop=propertyExpression[groupContext, true] { paramProps.add($prop.property); usedParams.add($prop.usedParams); }
		'BY'
		exprList=nonEmptyPropertyExpressionList[groupContext, true] 
		{ paramProps.addAll($exprList.props); usedParams.addAll($exprList.usedParams); }
	;



partitionPropertyDefinition[List<String> context, boolean dynamic] returns [LP property, List<Integer> usedParams]
@init {
	List<List<Integer>> usedSubParams = new ArrayList<List<Integer>>();
	List<LP<?>> paramProps = new ArrayList<LP<?>>();
	OrderType type = null;
	int groupExprCnt;
	boolean ascending = true;
	boolean useLast = true;
}
@after {
	if (parseState == ScriptingLogicsModule.State.PROP) {
		ScriptingLogicsModule.LPWithParams result = 
			self.addScriptedOProp(type, ascending, useLast, groupExprCnt, paramProps, usedSubParams);
		$property = result.property;
		$usedParams = result.usedParams;	
	}
}
	:	'PARTITION' ('SUM' {type = OrderType.SUM;} | 'PREV' {type = OrderType.PREVIOUS;})
		expr=propertyExpression[context, dynamic] { paramProps.add($expr.property); usedSubParams.add($expr.usedParams); }
		(	'BY'
			exprList=nonEmptyPropertyExpressionList[context, dynamic] { paramProps.addAll($exprList.props); usedSubParams.addAll($exprList.usedParams); }
		)?
		{ groupExprCnt = paramProps.size() - 1; }
		(	'ORDER' ('DESC' { ascending = false; } )?				
			orderList=nonEmptyPropertyExpressionList[context, dynamic] { paramProps.addAll($orderList.props); usedSubParams.addAll($orderList.usedParams); }
		)? 
		('WINDOW' 'EXCEPTLAST' { useLast = false; })?
	;


dataPropertyDefinition[boolean innerPD] returns [LP property]
@init {
	List<String> paramClassNames;
	String returnClass = null;
}
@after {
	if (parseState == ScriptingLogicsModule.State.PROP) {
		$property = self.addScriptedDProp(returnClass, paramClassNames, innerPD);
	}
}
	:	'DATA'
		retClass=classId { returnClass = $retClass.text; }
		'('
			classIds=classIdList { paramClassNames = $classIds.ids; }
		')'
	;



unionPropertyDefinition[List<String> context, boolean dynamic] returns [LP property, List<Integer> usedParams]
@init {
	List<LP<?>> paramProps;
	List<List<Integer>> usedSubParams;
	Union type = null;
}
@after {
	if (parseState == ScriptingLogicsModule.State.PROP) {
		ScriptingLogicsModule.LPWithParams result = self.addScriptedUProp(type, paramProps, usedSubParams);
		$property = result.property;
		$usedParams = result.usedParams;	
	}
}
	:	'UNION'
		(('MAX' {type = Union.MAX;}) | ('SUM' {type = Union.SUM;}) | ('OVERRIDE' {type = Union.OVERRIDE;}) | ('XOR' { type = Union.XOR;}) | ('EXCLUSIVE' {type = Union.EXCLUSIVE;}))
		exprList=nonEmptyPropertyExpressionList[context, dynamic] { paramProps = $exprList.props; usedSubParams = $exprList.usedParams; }	
	;


formulaPropertyDefinition returns [LP property]
@init {
	String className;
	String formulaText;
}
@after {
	if (parseState == ScriptingLogicsModule.State.PROP) {
		$property = self.addScriptedSFProp(className, formulaText);
	}
}
	:	'FORMULA' type=classId str=STRING_LITERAL { className = $type.text; formulaText = $str.text; }
	;


typePropertyDefinition returns [LP property] 
@init {
	String clsId = null;
	boolean bIs = false;
}
@after {
	if (parseState == ScriptingLogicsModule.State.PROP) {
		$property = self.addScriptedTypeProp(clsId, bIs);
	}	
}
	:	('IS' { bIs = true; } | 'AS')
		id=classId { clsId = $id.text; }
	;


propertyObject returns [LP property, String propName, List<String> innerContext]
@init {
	List<String> newContext = new ArrayList<String>(); 
}
	:	name=compoundID	{ if (parseState == ScriptingLogicsModule.State.PROP) 
							{$property = self.findLPByCompoundName($name.text); $propName = $name.text;} 
						} 
	|	'[' 
			(expr=propertyExpression[newContext, true] { $property = $expr.property; $innerContext = newContext; } 
		|	def=contextIndependentPD[true] { $property = $def.property; })
		']' 
	;


commonPropertySettings[LP property, String propertyName, String caption, List<String> namedParams, boolean isData] 
@init {
	String groupName = null;
	boolean isPersistent = false;	
}
@after {
	if (parseState == ScriptingLogicsModule.State.PROP) { 
		self.addSettingsToProperty(property, propertyName, caption, namedParams, groupName, isPersistent, isData);	
	}
} 
	: 	('IN' name=compoundID { groupName = $name.text; })?
		('PERSISTENT' { isPersistent = true; })?
	;


////////////////////////////////////////////////////////////////////////////////
//////////////////////////////// CONSTRAINT STATEMENT //////////////////////////
////////////////////////////////////////////////////////////////////////////////

constraintStatement 
@init {
	boolean checked = false;
	LP<?> prop = null;
	String message = null;
}
@after {
	if (parseState == ScriptingLogicsModule.State.PROP) { 
		self.addScriptedConstraint(prop, checked, message);	
	}
}
	:	'CONSTRAINT' ('CHECKED' { checked = true; })? 
		expr=propertyExpression[new ArrayList<String>(), true] { prop = $expr.property; }	
		'MSG' msg=STRING_LITERAL { message = $msg.text; }	 
	;


////////////////////////////////////////////////////////////////////////////////
///////////////////////////////// FOLLOWS STATEMENT ////////////////////////////
////////////////////////////////////////////////////////////////////////////////

followsStatement
@init {
	List<String> context;
	String mainProp;
	List<List<Integer>> usedParams = new ArrayList<List<Integer>>();
	List<LP<?>> props = new ArrayList<LP<?>>();
	List<Integer> options = new ArrayList<Integer>();
}
@after {
	if (parseState == ScriptingLogicsModule.State.PROP) {
		self.addScriptedFollows(mainProp, context.size(), options, props, usedParams);
	}
}
	:	prop=propertyWithNamedParams { mainProp = $prop.name; context = $prop.params; }	
		'=>' 
		firstExpr=propertyExpression[context, false] ('RESOLVE' type=followsResolveType)? 
		{ 
			props.add($firstExpr.property); usedParams.add($firstExpr.usedParams); 
			options.add(type == null ? PropertyFollows.RESOLVE_ALL : $type.type); 
		}
		(',' nextExpr=propertyExpression[context, false] ('RESOLVE' type=followsResolveType)? 
			{ 
		     	props.add($nextExpr.property); usedParams.add($nextExpr.usedParams); 
		     	options.add(type == null ? PropertyFollows.RESOLVE_ALL : $type.type); 
			}
		)*
	;	

followsResolveType returns [Integer type]
	:	lit=LOGICAL_LITERAL	{ $type = $lit.text.equals("TRUE") ? PropertyFollows.RESOLVE_TRUE : PropertyFollows.RESOLVE_FALSE; }
	|	'ALL'			{ $type = PropertyFollows.RESOLVE_ALL; }
	|	'NOTHING'		{ $type = PropertyFollows.RESOLVE_NOTHING; }
	;


////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// CHANGE STATEMENT ////////////////////////////
////////////////////////////////////////////////////////////////////////////////

writeOnChangeStatement
@init {
	boolean old = false;
	boolean anyChange = true;
	String mainPropName;
	List<String> context;
	LP<?> valueProp = null;
	List<Integer> valueUsedParams;
	LP<?> changeProp = null;
	List<Integer> changeUsedParams;
}
@after {
	if (parseState == ScriptingLogicsModule.State.PROP) { 
		self.addScriptedWriteOnChange(mainPropName, context.size(), old, anyChange, valueProp, valueUsedParams, changeProp, changeUsedParams);
	}
}
	:	prop=propertyWithNamedParams { mainPropName = $prop.name; context = $prop.params; }
		'<-'
		('OLD' { old = true; })?
		valueExpr=propertyExpression[context, false] { valueProp = $valueExpr.property; valueUsedParams = $valueExpr.usedParams; }
		'ON' ('CHANGE' | 'ASSIGN' { anyChange = false; }) 
		changeExpr=propertyExpression[context, false] { changeProp = $changeExpr.property; changeUsedParams = $changeExpr.usedParams; }
	;


////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// TABLE STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

tableStatement 
	:	't';


////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// INDEX STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

indexStatement
	:	'z';


////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// DESIGN STATEMENT ////////////////////////////
////////////////////////////////////////////////////////////////////////////////

designStatement returns [ScriptedFormView formView]
scope {
	ScriptedFormView design;
}
@init {
	boolean applyDefault = false;
}
@after {
}
	: 	'DESIGN'
		nameWithCaption=simpleNameWithCaption
		('FROM' 'DEFAULT' {applyDefault = true;} )?
		{
			if (inNavigatorParseState()) {
				$designStatement::design = $formView = self.createScriptedFormView($nameWithCaption.name, $nameWithCaption.caption, applyDefault);
			}
		}
		componentStatementBody[formView, formView == null ? null : formView.mainContainer]
	;

componentStatementBody [Object propertyReceiver, ComponentView parentComponent]
	: '{'
		( setObjectPropertyStatement[propertyReceiver]
		| positionComponentsStatement[parentComponent]
		| setupComponentStatement
		| setupGroupObjectStatement
		| addComponentStatement[parentComponent]
		| removeComponentStatement )*
	  '}'
	| ';'
	;

setupComponentStatement
	: comp=componentSelector[true] componentStatementBody[$comp.component, $comp.component]
	;

setupGroupObjectStatement
@init {
	GroupObjectView groupObject = null;
}
	: 'GROUP'
	  '('
		ID
		{
			if (inNavigatorParseState()) {
				groupObject = $designStatement::design.getGroupObject($ID.text);
			}
		}
	  ')' '{' setObjectPropertyStatement[groupObject]* '}'
	;

addComponentStatement[ComponentView parentComponent]
@init {
	boolean hasPosition = false;
	ComponentView insComp = null;
}
	: 'ADD' insSelector=componentSelector[false] { insComp = $insSelector.component; }
		( posDefinition=addPositionDefinition posSelector=componentSelector[true] { hasPosition = true; } )?
		{
			if (inNavigatorParseState()) {
				insComp = $designStatement::design.addComponent($insSelector.sid,
																insComp,
																hasPosition ? $posDefinition.position : InsertPosition.IN,
																hasPosition ? $posSelector.component : $parentComponent);
			}
		}
   	    componentStatementBody[insComp, insComp]
	;

removeComponentStatement
@init {
	boolean cascade = false;
}
	: 'REMOVE' compSelector=componentSelector[true] ('CASCADE' { cascade = true; } )? ';'
		{
			if (inNavigatorParseState()) {
				$designStatement::design.removeComponent($compSelector.component, cascade);
			}
		}
	;

componentSelector[boolean hasToExist] returns [String sid, ComponentView component]
	: 'PARENT' '(' child=componentSelector[true] ')'
		{
			if (inNavigatorParseState()) {
				$designStatement::design.getParentContainer($child.component);
			}
		}
	| 'PROPERTY' '(' prop=propertySelector ')' { $component = $prop.propertyView; }
	| mid=multiCompoundID
		{
			if (inNavigatorParseState()) {
				$sid = $mid.sid;
				$component = $designStatement::design.getComponentBySID($sid, hasToExist);
			}
		}
	;


propertySelector returns [PropertyDrawView propertyView = null]
	: pname=formPropertyName
		{
			if (inNavigatorParseState()) {
				$propertyView = $designStatement::design.getPropertyView($pname.text);
			}
		}
	| mappedProp=formMappedProperty
		{
			if (inNavigatorParseState()) {
				$propertyView = $designStatement::design.getPropertyView($mappedProp.name, $mappedProp.mapping);
			}
		}
	;

positionComponentsStatement[ComponentView parentComponent]
@init {
	boolean hasSecondComponent = false;
}
	: 'POSITION' compSelector1=componentSelector[true] constrDefinition=constraintDefinition ( compSelector2=componentSelector[true]  { hasSecondComponent = true; } )? ';'
    	{
		if (inNavigatorParseState()) {
	    		$designStatement::design.addIntersection($compSelector1.component,
	    							 					$constrDefinition.constraint,
	    							 					hasSecondComponent ? $compSelector2.component : parentComponent);
	    	}
    	}
	;

constraintDefinition returns [DoNotIntersectSimplexConstraint constraint]
	: 'TO' 'THE' 'LEFT' { $constraint = TOTHE_LEFT; }
	| 'TO' 'THE' 'RIGHT' { $constraint = TOTHE_RIGHT; }
	| 'TO' 'THE' 'BOTTOM' { $constraint = TOTHE_BOTTOM; }
	| 'TO' 'THE' 'RIGHTBOTTOM' { $constraint = TOTHE_RIGHTBOTTOM; }
	| 'TO' 'NOT' 'INTERSECT' { $constraint = DO_NOT_INTERSECT; }
	;

addPositionDefinition returns [InsertPosition position]
	: 'IN' { $position = InsertPosition.IN; }
	| 'BEFORE' { $position = InsertPosition.BEFORE; }
	| 'AFTER' { $position = InsertPosition.AFTER; }
	;


setObjectPropertyStatement[Object propertyReceiver] returns [String id, Object value]
	: ID '=' componentPropertyValue ';'  { setObjectProperty($propertyReceiver, $ID.text, $componentPropertyValue.value); }
	;

componentPropertyValue returns [Object value]
	: col=COLOR_LITERAL { $value = Color.decode($col.text); }
	| str=STRING_LITERAL { $value = self.transformStringLiteral($str.text); }
	| i=intLiteral { $value = $i.val; }
	| d=doubleLiteral { $value = $d.val; }
	| dim=dimensionsLiteral { $value = $dim.dimension; }
	| bool=booleanLiteral { $value=Boolean.valueOf($bool.text); }
	;

intLiteral returns [int val]
@init {
	boolean isMinus = false;
}
	: (MINUS {isMinus=true;})? ui=uintLiteral  { $val = (isMinus ? -1 : 1) * Integer.parseInt($ui.text); }
	;

doubleLiteral returns [double val]
@init {
	boolean isMinus = false;
}
	: (MINUS {isMinus=true;})? ud=udoubleLiteral { $val = (isMinus ? -1 : 1) * Double.parseDouble($ud.text); }
	;

booleanLiteral
	: LOGICAL_LITERAL
	;

dimensionsLiteral returns [Dimension dimension]
	: '{' x=intLiteral ',' y=intLiteral '}' { $dimension = new Dimension(Integer.parseInt($x.text), Integer.parseInt($y.text)); }
	;

////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////// COMMON /////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

propertyWithNamedParams returns [String name, List<String> params]
	:	propName=compoundID { $name = $propName.text; }
		'(' 
		list=idList { $params = $list.ids; }
		')'	
	;

parameter 
	:	ID | NUMBERED_PARAM
	;


simpleNameWithCaption returns [String name, String caption] 
	:	simpleName=ID { $name = $simpleName.text; }
		(captionStr=STRING_LITERAL { $caption = $captionStr.text; })?
	;
	
idList returns [List<String> ids] 
@init {
	ids = new ArrayList<String>();	
} 
	: (neIdList=nonEmptyIdList { ids = $neIdList.ids; })?
	;

classIdList returns [List<String> ids]
@init {
	ids = new ArrayList<String>();
}
	:	((firstClassName=classId { ids.add($firstClassName.text); })
		(',' className=classId { ids.add($className.text); })*)?
	;

compoundIdList returns [List<String> ids] 
@init {
	ids = new ArrayList<String>();	
} 
	: (neIdList=nonEmptyCompoundIdList { ids = $neIdList.ids; })?
	;

nonEmptyIdList returns [List<String> ids]
@init {
	ids = new ArrayList<String>(); 
}
	:	firstId=ID	{ $ids.add($firstId.text); }
		(',' nextId=ID	{ $ids.add($nextId.text); })*
	;

nonEmptyCompoundIdList returns [List<String> ids]
@init {
	ids = new ArrayList<String>();
}
	:	firstId=compoundID	{ $ids.add($firstId.text); }
		(',' nextId=compoundID	{ $ids.add($nextId.text); })*
	;

parameterList returns [List<String> ids]
@init {
	ids = new ArrayList<String>();
}
	:	(firstParam=parameter	 { $ids.add($firstParam.text); }
		(',' nextParam=parameter { $ids.add($nextParam.text); })* )?
	;


propertyExpressionList[List<String> context, boolean dynamic] returns [List<LP<?>> props, List<List<Integer>> usedParams] 
@init {
	$props = new ArrayList<LP<?>>();
	$usedParams = new ArrayList<List<Integer>>(); 
}
	:	(neList=nonEmptyPropertyExpressionList[context, dynamic] { $props = $neList.props; $usedParams = $neList.usedParams; })?
	;
	

nonEmptyPropertyExpressionList[List<String> context, boolean dynamic] returns [List<LP<?>> props, List<List<Integer>> usedParams]
@init {
	$props = new ArrayList<LP<?>>();
	$usedParams = new ArrayList<List<Integer>>(); 
}
	:	first=propertyExpression[context, dynamic] { $props.add($first.property); $usedParams.add($first.usedParams); }
		(',' next=propertyExpression[context, dynamic] { $props.add($next.property); $usedParams.add($next.usedParams);})* 
	; 

literal returns [LP property]
@init {
	ScriptingLogicsModule.ConstType cls = null;
	String text = null;
}
@after {
	if (parseState == ScriptingLogicsModule.State.PROP) { 
		$property = self.addConstantProp(cls, text);	
	}
}
	: 	strInt=uintLiteral	{ cls = ScriptingLogicsModule.ConstType.INT; text = $strInt.text; }
	|	strReal=udoubleLiteral	{ cls = ScriptingLogicsModule.ConstType.REAL; text = $strReal.text; }
	|	str=STRING_LITERAL	{ cls = ScriptingLogicsModule.ConstType.STRING; text = $str.text; }  
	|	str=LOGICAL_LITERAL	{ cls = ScriptingLogicsModule.ConstType.LOGICAL; text = $str.text; }
	|	strEnum=strictCompoundID{ cls = ScriptingLogicsModule.ConstType.ENUM; text = $strEnum.text; } 
	;
	
classId 
	:	compoundID | PRIMITIVE_TYPE
	;

compoundID
	:	(ID '.')? ID
	;

strictCompoundID
	:	ID '.' ID
	;
	
multiCompoundID returns [String sid]
	:	id=ID { $sid = $id.text; } ('.' cid=ID { $sid = $sid + "." + $cid.text; } )*
	;

udoubleLiteral
	:	POSITIVE_DOUBLE_LITERAL
	; 
		

uintLiteral
	:	UINT_LITERAL
	;		



/////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////// LEXER //////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////
	
fragment NEWLINE	:	'\r'?'\n'; 
fragment SPACE		:	(' '|'\t');
fragment STR_LITERAL_CHAR	: '\\\'' | ~('\r'|'\n'|'\'');	 // overcomplicated due to bug in ANTLR Works
fragment DIGITS		:	('0'..'9')+;
fragment HEX_DIGIT	: 	'0'..'9' | 'a'..'f' | 'A'..'F';

PRIMITIVE_TYPE  :	'INTEGER' | 'DOUBLE' | 'LONG' | 'BOOLEAN' | 'DATE' | 'STRING[' DIGITS ']' | 'ISTRING[' DIGITS ']';		
LOGICAL_LITERAL :	'TRUE' | 'FALSE';		
ID          	:	('a'..'z'|'A'..'Z')('a'..'z'|'A'..'Z'|'_'|'0'..'9')*;
WS				:	(NEWLINE | SPACE) { $channel=HIDDEN; };
STRING_LITERAL	:	'\'' STR_LITERAL_CHAR* '\'';
COLOR_LITERAL 	:	'#' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT;
COMMENTS		:	('//' .* '\n') { $channel=HIDDEN; };
UINT_LITERAL 	:	DIGITS;
POSITIVE_DOUBLE_LITERAL	: 	DIGITS '.' DIGITS;	  
NUMBERED_PARAM	:	'$' DIGITS;
EQ_OPERAND		:	('==') | ('!=');
REL_OPERAND		: 	('<') | ('>') | ('<=') | ('>=');
MINUS			:	'-';
PLUS			:	'+';
MULT_OPERAND	:	('*') | ('/');