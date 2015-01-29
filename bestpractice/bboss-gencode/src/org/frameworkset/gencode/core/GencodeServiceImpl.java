package org.frameworkset.gencode.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.frameworkset.gencode.entity.Annotation;
import org.frameworkset.gencode.entity.Field;
import org.frameworkset.gencode.entity.Method;
import org.frameworkset.gencode.entity.MethodParam;
import org.frameworkset.gencode.entity.ModuleMetaInfo;

import bboss.org.apache.velocity.Template;
import bboss.org.apache.velocity.VelocityContext;

import com.frameworkset.common.poolman.DBUtil;
import com.frameworkset.common.poolman.sql.ColumnMetaData;
import com.frameworkset.common.poolman.sql.TableMetaData;
import com.frameworkset.orm.engine.EngineException;
import com.frameworkset.orm.engine.model.NameFactory;
import com.frameworkset.orm.engine.model.NameGenerator;
import com.frameworkset.util.SimpleStringUtil;
import com.frameworkset.util.VelocityUtil;

/**
 * 生成代码业务组件
 * @author yinbp
 *
 */
public class GencodeServiceImpl {
	private static final Logger log = Logger.getLogger(GencodeServiceImpl.class);
	/**
	 * 代码存放目录
	 */
	private File rootdir;
	private File resourcedir;
	private File javaSourceDir;
	private File javaEntiySourceDir;
	private File javaServiceSourceDir;
	private File javaActionSourceDir;
	private File jspSourceDir;
	private File mvcConfDir;
	private File webxmlFile;
	private ModuleMetaInfo moduleMetaInfo;
	private String exceptionName;
	private String entityName;
	private String entityParamName;
	
	public String genCode(ModuleMetaInfo moduleMetaInfo)
	{
		this.moduleMetaInfo = moduleMetaInfo; 
		this.init();
		genJavaSource();
		return "success";
	}
	
	private void init()
	{
		File f = new File(moduleMetaInfo.getSourcedir());
		if(!f.exists())
		{
			f.mkdirs();
		}
		else if(moduleMetaInfo.isClearSourcedir())
		{
			f.delete();
			f.mkdirs();
		}
		this.rootdir = f;
		String modulePackage = moduleMetaInfo.getPackagePath().replace(".", "/") ;
		modulePackage = modulePackage.endsWith("/") ? modulePackage + moduleMetaInfo.getModuleName():
			modulePackage + "/" + moduleMetaInfo.getModuleName();
		javaSourceDir = new File(this.rootdir,"src/"+modulePackage);
		if(!javaSourceDir.exists())
		{
			javaSourceDir.mkdirs();
		}
		
		javaEntiySourceDir = new File(this.javaSourceDir,  "entity");
		if(!javaEntiySourceDir.exists())
		{
			javaEntiySourceDir.mkdirs();
		}
		javaServiceSourceDir = new File(this.javaSourceDir,"service");
		if(!javaServiceSourceDir.exists())
		{
			javaServiceSourceDir.mkdirs();
		}
		javaActionSourceDir = new File(this.javaSourceDir,"action");
		if(!javaActionSourceDir.exists())
		{
			javaActionSourceDir.mkdirs();
		}
		resourcedir = new File(this.rootdir,"resources");
		if(!resourcedir.exists())
		{
			resourcedir.mkdirs();
		}
		
		jspSourceDir = new File(this.rootdir,"WebRoot/"+ moduleMetaInfo.getModuleName());
		if(!jspSourceDir.exists())
		{
			jspSourceDir.mkdirs();
		}
		mvcConfDir = new File(this.rootdir,"WebRoot/WEB-INF/conf/"+ moduleMetaInfo.getModuleName());
		if(!mvcConfDir.exists())
		{
			mvcConfDir.mkdirs();
		}
		webxmlFile = new File(this.rootdir,"WebRoot/WEB-INF/web.xml");
		if(!webxmlFile.exists())
		{
			try {
				webxmlFile.createNewFile();
			} catch (IOException e) {
				log.error("Create web.xml failed:"+webxmlFile.getAbsolutePath(),e);
			}
		}
		
	}
	private String convertType(String type)
	{
		if(type.equals("BigDecimal"))
		{
			return "long";
		}
		else if(type.equals("Date"))
		{
			return "Timestamp";
		}
		else
			return type;
	}
	private Field primaryField ;
	
	private List<Field> getFields(TableMetaData tableMeta)
	{
		Set<ColumnMetaData> columns = tableMeta.getColumns();
		if(columns.size() > 0)
		{
			 
			List<Field> fs = new ArrayList<Field>();
			
			for(ColumnMetaData c:columns)
			{
				Field f = new Field();
				f.setType(convertType(c.getSchemaType().getJavaType()));
				boolean isp = false;
				if(this.moduleMetaInfo.isAutogenprimarykey() && tableMeta.getPrimaryKeyMetaData(c.getColumnName()) != null)
				{
					Annotation anno = new Annotation();
					anno.setName("PrimaryKey");
					f.addAnnotation(anno);
					isp = true;
				}
				
		         try
		         {
		        	 List<String> inputs = new ArrayList<String>(2);
			         inputs.add(c.getColumnName().toLowerCase());
			         inputs.add( NameGenerator.CONV_METHOD_JAVANAME);
		        	 String mfieldName = NameFactory.generateName(NameFactory.JAVA_GENERATOR,
		                                                 inputs,false);
		        	 inputs = new ArrayList<String>(2);
			         inputs.add(c.getColumnName().toLowerCase());
			         inputs.add( NameGenerator.CONV_METHOD_JAVAFIELDNAME);
		        	 String fieldName = NameFactory.generateName(NameFactory.JAVA_GENERATOR,
                             inputs,false);
		        	 f.setMfieldName(mfieldName);
		        	 f.setFieldName(fieldName);
		        	 if(isp)
		        		 this.primaryField = f;
		        	 fs.add(f);
		         }
		         catch (EngineException e)
		         {
		             log.error(e.getMessage(), e);
		         }
				
			}
			return fs;
			
		}
		return null;
	}
	
	public void genJavaSource()
	{
		
		
		
		
		String entityName = this.moduleMetaInfo.getEntityName(); 
		if(SimpleStringUtil.isEmpty(entityName))
		{
			 
			 List inputs = new ArrayList(2);
	         inputs.add(moduleMetaInfo.getTableName());
	         inputs.add( NameGenerator.CONV_METHOD_JAVANAME);
	         
	         try
	         {
	        	 entityName = NameFactory.generateName(NameFactory.JAVA_GENERATOR,
	                                                 inputs,this.moduleMetaInfo.isIgnoreEntityFirstToken());
	             
	         }
	         catch (EngineException e)
	         {
	             log.error(e.getMessage(), e);
	         }
		}
		String entityJavaName = entityName + ".java";
		String serviceInfJavaName = entityName + "Service.java";
		String serviceImplJavaName = entityName + "ServiceImpl.java";
		String controllerJavaName = entityName + "Controller.java";
		String exceptionJavaName = entityName + "Exception.java";
		this.exceptionName = entityName + "Exception";
		this.entityName = entityName;
		this.entityParamName = (entityName.charAt(0)+"").toLowerCase()  + entityName.substring(1);
		
		File entity = new File(this.javaEntiySourceDir,entityJavaName);
		if(entity.exists())
		{
			entity.delete();
		}
		File exception = new File(this.javaServiceSourceDir,exceptionJavaName);
		if(exception.exists())
		{
			exception.delete();
		}
		File serviceInf = new File(this.javaServiceSourceDir,serviceInfJavaName);
		if(serviceInf.exists())
		{
			serviceInf.delete();
		}
		File serviceImpl = new File(this.javaServiceSourceDir,serviceImplJavaName);
		if(serviceImpl.exists())
		{
			serviceImpl.delete();
		}
		File controller = new File(this.javaActionSourceDir,controllerJavaName);
		if(controller.exists())
		{
			controller.delete();
		}
		try {
			entity.createNewFile();
			serviceInf.createNewFile();
			serviceImpl.createNewFile();
			controller.createNewFile();
			exception.createNewFile();
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String date = format.format(new Date());			
			TableMetaData tableMeta = DBUtil.getTableMetaData(this.moduleMetaInfo.getDatasourceName(), this.moduleMetaInfo.getTableName());
 
			 genEntity(  entityName,  date,  "v1.0","yinbp","sany","服务实体类",entity,tableMeta);
			 genServiceInf(  entityName + "Service",date,"v1.0","yinbp","sany","服务管理接口", serviceInf);
			 genException(entityName + "Exception",date,"v1.0","yinbp","sany","异常处理类",exception);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private void genEntity(String entityName,String date,String version,String author,String company,String description,File entity,TableMetaData tableMeta) throws Exception
	{
		 List<Field> fields = getFields( tableMeta);
		 List<String> imports = evalImport(fields);
		 Template entitytempalte = VelocityUtil.getTemplate("gencode/entityjava.vm");
		 VelocityContext context = new VelocityContext();
		 context.put("fields", fields);
		 String entityPackageInfo = this.moduleMetaInfo.getPackagePath() + "." + this.moduleMetaInfo.getModuleName()+".entity";
		 context.put("package", entityPackageInfo);
		 context.put("imports", imports);
		 context.put("classname", entityName);
		 context.put("description", description);
		 context.put("company", company);
		 context.put("gendate", date);
		 context.put("author", author);
		 context.put("version", version);
	 
		 writFile(context,entitytempalte,entity,this.moduleMetaInfo.getEncodecharset());
		
	}
	
	
	
	private void genServiceInf(String serviceinfName,String date,String version,String author,String company,String description,File serviceInf) throws Exception
	{
		 
		 List<String> imports = evalServiceInfImport();
		 Template serviceinftempalte = VelocityUtil.getTemplate("gencode/serviceinfjava.vm");
		 VelocityContext context = new VelocityContext();
		 context.put("fields", null);
		 String entityPackageInfo = this.moduleMetaInfo.getPackagePath() + "." + this.moduleMetaInfo.getModuleName()+".service";
		 context.put("package", entityPackageInfo);
		 context.put("imports", imports);
		 context.put("classname", serviceinfName);
		 context.put("description", description);
		 context.put("company", company);
		 context.put("gendate", date);
		 context.put("author", author);
		 context.put("version", version);
		
		 List<Method> methods = getInfMethods();
		 context.put("methods", methods);
	 
		 writFile(context,serviceinftempalte,serviceInf,this.moduleMetaInfo.getEncodecharset());
		
	}
	
	private void genServiceImpl(String serviceName,String serviceInfname,String date,String version,String author,String company,String description,File service) throws Exception
	{
		 
		 List<String> imports = evalServiceInfImport();
		 Template serviceinftempalte = VelocityUtil.getTemplate("gencode/serviceimpljava.vm");
		 VelocityContext context = new VelocityContext();
		 context.put("fields", null);
		 String entityPackageInfo = this.moduleMetaInfo.getPackagePath() + "." + this.moduleMetaInfo.getModuleName()+".service";
		 context.put("package", entityPackageInfo);
		 context.put("imports", imports);
		 context.put("classname", serviceName);
		 context.put("description", description);
		 context.put("company", company);
		 context.put("gendate", date);
		 context.put("author", author);
		 context.put("version", version);
		
		 List<Method> methods = getInfMethods();
		 context.put("methods", methods);
	 
		 writFile(context,serviceinftempalte,service,this.moduleMetaInfo.getEncodecharset());
		
	}
	
	private List<Method> getInfMethods( ) {
		List<String> exceptions = new ArrayList<String>();
		exceptions.add(exceptionName);
		
		List<Method> methods = new ArrayList<Method>();
		Method add = new Method();//定义添加方法
		add.setMethodname("add"+entityName);
		add.setReturntype("void");
		
		add.setExceptions(exceptions);
		List<MethodParam> params = new ArrayList<MethodParam>();
		MethodParam param = new MethodParam();
		param.setType(this.entityName);
		param.setName(this.entityParamName);
		params.add(param);
		add.setParams(params);
		methods.add(add);
		
		
		
		
		Method delete = new Method();//定义删除方法
		delete.setMethodname("delete"+entityName);
		delete.setReturntype("void");		
		params = new ArrayList<MethodParam>();
		param = new MethodParam();
		if(primaryField != null)
		{
			param.setType(primaryField.getType());
			param.setName(primaryField.getFieldName());
		}
		else
		{
			param.setType("String");
			param.setName("id");
		}
		params.add(param);
		delete.setExceptions(exceptions);		
		delete.setParams(params);
		methods.add(delete);
		
		Method update = new Method();//定义修改方法
		update.setMethodname("update"+entityName);
		update.setReturntype("void");		
		params = new ArrayList<MethodParam>();
		param = new MethodParam();
		param.setType(this.entityName);
		param.setName(this.entityParamName);
		params.add(param);
		update.setExceptions(exceptions);		
		update.setParams(params);
		methods.add(update);
		
		Method get = new Method();//定义获取方法
		get.setMethodname("get"+entityName);
		get.setReturntype(entityName);		
		params = new ArrayList<MethodParam>();
		param = new MethodParam();
		
		if(primaryField != null)
		{
			param.setType(primaryField.getType());
			param.setName(primaryField.getFieldName());
		}
		else
		{
			param.setType("String");
			param.setName("id");
		}
		params.add(param);
		get.setExceptions(exceptions);		
		get.setParams(params);
		methods.add(get);
		
		Method paginequery = new Method();//定义获取方法
		paginequery.setMethodname("query"+entityName+"s");
		get.setReturntype("ListInfo");		
		params = new ArrayList<MethodParam>();
		param = new MethodParam();
		
		
		param.setType("Map");
		param.setName("conditions");
		params.add(param);
		
		param = new MethodParam();
		param.setType("long");
		param.setName("offset");
		params.add(param);
		param = new MethodParam();
		param.setType("int");
		param.setName("pagesize");
		params.add(param);
		get.setExceptions(exceptions);		
		get.setParams(params);
		methods.add(get);
		return methods;
	}
	
	private void writFile(  VelocityContext context,Template tempalte,File file,String charset)throws Exception
	{
		FileOutputStream out = null;
		 OutputStreamWriter writer = null;
		 try
		 {
			 out = new FileOutputStream(file);
			 writer = new OutputStreamWriter(out,charset);
			 tempalte.merge(context, writer);
			 writer.flush();
		 }
		 catch(Exception e)
		 {
			 throw e;
		 }
		 finally
		 {
			 if(out != null)
				try {
					out.close();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			 if(writer != null)
				try {
					writer.close();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		 }
		 
	}

	private void genException(String exceptionJavaName,String date,String version,String author,String company,String description,File exception) throws Exception
	{
		 
		 List<String> imports = null;
		 Template serviceinftempalte = VelocityUtil.getTemplate("gencode/exception.vm");
		 VelocityContext context = new VelocityContext();
		 context.put("fields", null);
		 String entityPackageInfo = this.moduleMetaInfo.getPackagePath() + "." + this.moduleMetaInfo.getModuleName()+".service";
		 context.put("package", entityPackageInfo);
		 context.put("imports", imports);
		 context.put("classname", exceptionJavaName);
		 context.put("description", description);
		 context.put("company", company);
		 context.put("gendate", date);
		 context.put("author", author);
		 context.put("version", version);
		 writFile(context,serviceinftempalte,exception,this.moduleMetaInfo.getEncodecharset());
		 
		
	}
	
	private List<String> evalImport(List<Field> fields) {
		List<String> imports = new ArrayList<String>();
		for(Field f:fields)
		{
	         if(f.getType().equals("Timestamp"))
	         {
	        	 if(!imports.contains("java.sql.Timestamp"))
	        		 imports.add("java.sql.Timestamp");
	         }
	         
		}
		return imports;
	}
	private List<String> evalServiceInfImport( ) {
		List<String> imports = new ArrayList<String>();
		imports.add(this.moduleMetaInfo.getPackagePath() + "." + this.moduleMetaInfo.getModuleName()+".entity.*");
		imports.add("com.frameworkset.util.ListInfo");
		return imports;
	}


	public static void main(String[] args)
	{
		 List inputs = new ArrayList(2);
         inputs.add("td_tableinfo_name");
         inputs.add( NameGenerator.CONV_METHOD_UNDERSCORE);
         try
         {
             String javaName = NameFactory.generateName(NameFactory.JAVA_GENERATOR,
                                                 inputs,false);
             System.out.println(javaName);
         }
         catch (EngineException e)
         {
             log.error(e, e);
         }
		
           inputs = new ArrayList(2);
         inputs.add("td_tableinfo_name");
         inputs.add( NameGenerator.CONV_METHOD_UNDERSCORE_OMIT_SCHEMA);
         try
         {
             String javaName = NameFactory.generateName(NameFactory.JAVA_GENERATOR,
                                                 inputs,false);
             System.out.println(javaName);
         }
         catch (EngineException e)
         {
             log.error(e, e);
         }
         inputs = new ArrayList(2);
         inputs.add("td_tableinfo_name");
         inputs.add( NameGenerator.CONV_METHOD_JAVANAME);
         try
         {
             String javaName = NameFactory.generateName(NameFactory.JAVA_GENERATOR,
                                                 inputs,false);
             System.out.println(javaName);
         }
         catch (EngineException e)
         {
             log.error(e, e);
         }
         inputs = new ArrayList(2);
         inputs.add("td_tableinfo_name");
         inputs.add( NameGenerator.CONV_METHOD_NOCHANGE);
         try
         {
             String javaName = NameFactory.generateName(NameFactory.JAVA_GENERATOR,
                                                 inputs,false);
             System.out.println(javaName);
         }
         catch (EngineException e)
         {
             log.error(e, e);
         }
	}

}
