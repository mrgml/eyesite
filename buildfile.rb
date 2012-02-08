
require 'buildr/jaxb_xjc'

GROUP = "CSG"  
VERSION_NUMBER = '0.1.0-SNAPSHOT'
UPLOAD_LOCATION = 'sftp://assure:assure@magpie/opt/arantech/conf/eyesite'
  
JAVAX_MAIL = ['javax.mail:mail:jar:1.4']
JAVAX_ACTIVATION = ['javax.activation:activation:jar:1.1']
XERCES = ['xerces:xercesImpl:jar:2.10.0']
MOCKITO_ALL = ['org.mockito:mockito-all:jar:1.8.5']

# download.java.net here is for downloading mail (and maybe activation)
repositories.remote << "http://www.ibiblio.org/maven2" << "http://download.java.net/maven/2"
repositories.release_to = UPLOAD_LOCATION

desc "eyesite"

define 'eyesite' do
  LIB = FileList["lib/**"]
  project.version = VERSION_NUMBER
  project.group = GROUP
  touchpoint_location=path_to('Users','garylyons','Documents','workspace','touchpoint_5_1_br')
    
  puts "project info:"
  puts "project.version is: #{project.version}"
  puts "project.inspect is: #{project.inspect}"
  puts "compile.dependencies is: #{compile.dependencies}"
  
  test.using :testng
  
  compile.from compile_jaxb(_('src/main/resources/eyeSiteReports.xsd'), 
                            "-quiet", 
                            :package => "com.arantech.eyesite.bindings")
                            
  compile.with(JAVAX_MAIL, JAVAX_ACTIVATION, XERCES)  
  
  classpath_libs = []
  LIB.each { |lib_file| classpath_libs << "lib/" + lib_file.split("/").last }
  puts "Here's what classpath_libs looks like: "
  puts classpath_libs.join(" ")
  MAIN_CLASS = "com.arantech.eyesite.ReportRunner"
  
  manifest["Manifest-Version: 0.1"]
  manifest["Main-Class"]= MAIN_CLASS
  manifest["Class-Path"] = classpath_libs.join(" ")
  package(:jar)
  
  # This task is required to upload the libs to the correct location
  # It's probably not the way to go ultimately, it's likely better to tar;gzip all the jar's for delivery
  task 'upload' => package do
    LIB.each do |lib_file| 
      puts "file is #{file(lib_file).to_s}"
      uri = "sftp://assure:assure@magpie/opt/arantech/conf/eyesite/" +  
        "#{GROUP}/#{project.inspect.split('"')[1]}/#{VERSION_NUMBER}/" + lib_file.split('/').last
      URI(uri).upload(file(lib_file))
    end
  end
  
end



