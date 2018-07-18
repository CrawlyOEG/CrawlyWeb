package crawly.controllers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.base.Optional;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObject;
import com.optimaize.langdetect.text.TextObjectFactory;
import com.tfg.Extraccion.ExtractionMode;
import com.tfg.Extraccion.RangeExtraction;
import com.tfg.Extraccion.ReadPDF;
import com.tfg.LibrairyConection.EstructuraDelPDF;
import com.tfg.LibrairyConection.LibrairyConnection;

import alandb.Application;
import zenododb.ApplicationZenodo;

@Controller
public class MainController {

	@Autowired
	private Environment env;

	@RequestMapping("/")
	public String index() {
		return "index.html";
	}

	@RequestMapping(value = "/uploadFile2", method = RequestMethod.POST)
	@ResponseBody
	public Response uploadFile2(
			@RequestParam("uploadfile2") ArrayList<MultipartFile> uploadfile2) {
		double[] respuesta = null;
		String filepath = "";
		for (MultipartFile d : uploadfile2) {
			String filename = d.getOriginalFilename();
			System.out.println(filename);
		}
		return null;
	}

	@RequestMapping(value = "/checkPDF", method = RequestMethod.POST)
	@ResponseBody
	public Response checkPDF(
			@RequestParam("uploadfile") MultipartFile uploadfile,
			@RequestParam("modePDF") String modePDF,
			@RequestParam("fixPDF") String fixTextString,
			@RequestParam("allResources") String allResourcesString,
			@RequestParam("importantNumbers") String importantNumbers,
			HttpServletResponse response) {
		Boolean fixText = Boolean.valueOf(fixTextString);
		Boolean allResources = Boolean.valueOf(allResourcesString);
		String filepath = "";
		String rutaOutputFolder = env.getProperty("crawly.paths.pdfFiles");
		File outputfolder = new File(rutaOutputFolder);
		List<RangeExtraction> numerosMarcadores = new ArrayList<>();
		List<RangeExtraction> numerosPaginas = new ArrayList<>();
		try {
			String filename = uploadfile.getOriginalFilename();
			filepath = Paths.get(crearArchivo(filename,"crawly.paths.pdfFiles")).toString();
			File miArchivo = new File(filepath);
			BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(miArchivo));
			stream.write(uploadfile.getBytes());
			stream.close();
			ExtractionMode modoExtraccion = ExtractionMode.COMPLETE;
			if(modePDF=="butPages") {
				modoExtraccion = ExtractionMode.PAGES;
				numerosPaginas = comprobacionDeArgumentos(importantNumbers);
			}
			if(modePDF=="butBook") {
				modoExtraccion = ExtractionMode.BOOKMARK;
				numerosMarcadores = comprobacionDeArgumentos(importantNumbers);
			}
			String rutaCarpeta = new ReadPDF(miArchivo,modoExtraccion,numerosMarcadores,numerosPaginas,fixText,outputfolder,false).run();
			File directoryToZip = new File(rutaCarpeta);
			List<File> fileList = new ArrayList<File>();
			getAllFiles(directoryToZip, fileList);
			String zipDefinitivo = writeZipFile(directoryToZip, fileList);
			return new Response(zipDefinitivo);
//			File estaDefinitiva = new File(zipDefinitivo);
//			byte[] aPasar = readContentIntoByteArray(estaDefinitiva);
//			response.setHeader("Content-Disposition", "attachment; filename=download.zip");
//			response.setHeader("Content-Type", "application/zip");
//			response.getOutputStream().write(aPasar);
		}catch (Exception e) {
			File borrar = new File(filepath);
			if(borrar.exists())
				borrar.delete();
			System.out.println(e.getMessage());
			return new Response("401");
			//return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}
	
	@RequestMapping(value = "checkPDF/download/{file}", method = RequestMethod.GET)
	@ResponseBody
	public void downloadFile(
			@PathVariable("file") String file, HttpServletResponse response) throws Exception {
		String rutaOutputFolder = env.getProperty("crawly.paths.loLibrairy") + "//";;
		File licenseFile = new File (rutaOutputFolder + file + ".zip");
	    InputStream is = new FileInputStream(licenseFile);
	    // set file as attached data and copy file data to response output stream
	    response.setHeader("Content-Disposition", "attachment; filename=\"" + file + ".zip\"");
	    FileCopyUtils.copy(is, response.getOutputStream());
	    // delete file on server file system
	    licenseFile.delete();
	    // close stream and return to view
	    response.flushBuffer();
	}

	@RequestMapping(value = "/makeModel", method = RequestMethod.POST)
	@ResponseBody
	public Response makeModel(@RequestParam("urlLibrairy") String urlLibrairy,
			@RequestParam("user") String user,
			@RequestParam("password") String password,
			@RequestParam("topic") String topic,
			@RequestParam("correo") String correo,
			@RequestParam("nDocuments") String nDocuments) throws InvalidPasswordException, IOException {
		System.out.println("Nos llega librapeticion");
		String aaa = new LibrairyConnection("", urlLibrairy, urlLibrairy, user, password).comprobarConexion();
		if(!aaa.equals("")) {
			return new Response("401");
		}
		Thread thread = new Thread(new Runnable()
		{
		   public void run()
		   {
			   try {
				functionThread(user, password, topic, correo, nDocuments, urlLibrairy);
			} catch (InvalidPasswordException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		   }
		});
		thread.start(); 
		return new Response("201");
	}

	private void functionThread(String user, String password, String topic, String correo, String nDocuments, String urlLibrairy) throws InvalidPasswordException, IOException {
		String rutaOutputFolder = env.getProperty("crawly.paths.loLibrairy") + "//";
		String carpetaDarksky = crearCarpeta("Darksky");
		String carpetaZenodo = crearCarpeta("Zenodo");
		int numeroDocumentos = Integer.parseInt(nDocuments);
		new Application(carpetaDarksky,topic).run();
		new ApplicationZenodo(carpetaZenodo,topic).run();
		File nuevaZenodo = new File(carpetaZenodo);
		File nuevaDark = new File(carpetaDarksky);
		List<RangeExtraction> numeros = new ArrayList<RangeExtraction>();
		boolean fixText = false;
		List<EstructuraDelPDF> listaTexto = new ArrayList<EstructuraDelPDF>();
		int i = 0;
		for(File a: nuevaZenodo.listFiles()) {
			if(i>numeroDocumentos)
				break;
			ReadPDF informacionPDF = new ReadPDF(a,ExtractionMode.COMPLETE,numeros,numeros,fixText,nuevaZenodo,true);
			String textoPDF = informacionPDF.obtainOnlyText();
			EstructuraDelPDF meter = new EstructuraDelPDF(a.getName(), textoPDF);
			listaTexto.add(meter);
			i++;
		}
		for(File a: nuevaDark.listFiles()) {
			if(i>numeroDocumentos)
				break;
			ReadPDF informacionPDF = new ReadPDF(a,ExtractionMode.COMPLETE,numeros,numeros,fixText,nuevaDark,true);
			String textoPDF = informacionPDF.obtainOnlyText();
			EstructuraDelPDF meter = new EstructuraDelPDF(a.getName(), textoPDF);
			listaTexto.add(meter);
		}
		LibrairyConnection paraUsar = new LibrairyConnection(listaTexto,"",topic,urlLibrairy,urlLibrairy,user,password,correo);
		paraUsar.sacarTopicos();
	}
	
	private String crearCarpeta(String argumento) {
		String nuevaRuta = env.getProperty("crawly.paths.loLibrairy") + "\\" + argumento;
		int i = 1;
		String intentar = nuevaRuta;
		while(!(new File(intentar)).mkdirs()) {
			intentar = nuevaRuta + "(" + i + ")";
			i++;
		}
		return intentar + "\\";
	}

	@RequestMapping(value = "/uploadFile", method = RequestMethod.POST)
	@ResponseBody
	public Response uploadFile(
			@RequestParam("uploadfile") MultipartFile uploadfile) {
		double[] respuesta = null;
		String filepath = "";
		try {
			String filename = uploadfile.getOriginalFilename();
			filepath = Paths.get(crearArchivo(filename,"crawly.paths.uploadedFiles")).toString();
			File miArchivo = new File(filepath);
			BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(miArchivo));
			stream.write(uploadfile.getBytes());
			stream.close();
			String textoAPasar = leemosElPDF(filepath);
			if(textoAPasar == null) {
				return new Response("IT IS NOT POSSIBLE TO READ THE PDF", respuesta, 1);
			}
			if(!textoIngles(textoAPasar)) {
				return new Response("THE PDF IS NOT IN ENGLISH OR DOES NOT CONTAIN ENOUGHT INFORMATION", respuesta, 1);
			}
			respuesta = comprobarArchivo(textoAPasar);
			if(miArchivo.delete()){
				System.out.println(miArchivo.getName() + " is deleted!");
			}else{
				System.out.println("Delete operation is failed.");
			}
		}
		catch (Exception e) {
			File borrar = new File(filepath);
			e.printStackTrace();
			if(borrar.exists())
				borrar.delete();
			System.out.println(e.getMessage());
			//return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			return new Response("BAD REQUEST", null, 1);
		}
		if(respuesta == null) {
			return new Response("BAD REQUEST", respuesta, 1);
		}
		return new Response("DONE", respuesta, 0);
	} 

	private String leemosElPDF(String filepath) {
		File file = new File(filepath);
		try {
			return new ReadPDF(file).obtainOnlyText();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	private double[] comprobarArchivo(String textoPDF) {
		String user = "jgalan";
		String password = "oeg2018";
		String keyWord = "light";
		String urlSpace = "http://librairy.linkeddata.es/jgalan-space/";
		String urlTopics = "http://librairy.linkeddata.es/jgalan-topics/";
		LibrairyConnection paraComprobar = new LibrairyConnection(keyWord,urlTopics,urlSpace,user,password);
		return paraComprobar.comprobarArchivo(textoPDF);
	}

	private boolean textoIngles(String myTexto) {
		List<LanguageProfile> languageProfiles;
		try {
			languageProfiles = new LanguageProfileReader().readAllBuiltIn();
			LanguageDetector languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard())
					.withProfiles(languageProfiles)
					.build();
			TextObjectFactory textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText();
			TextObject textObject = textObjectFactory.forText(myTexto);
			Optional<LdLocale> lang = languageDetector.detect(textObject);
			String resultado = lang.toString();
			String elverdad = resultado.substring(resultado.length()-3, resultado.length()-1);
			if(elverdad.equals("en") || resultado.equals("Optional.absent()"))
				return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	private String crearArchivo(String argumento, String argument) {
		String probar = argumento.substring(0, argumento.length()-4);
		String nuevaRuta = env.getProperty(argument) + "\\" + probar + ".pdf";
		int i = 1;
		String intentar = nuevaRuta;
		while((new File(intentar)).exists()) {
			intentar = env.getProperty(argument) + "\\" + probar +  "(" + i + ")" + ".pdf";
			i++;
		}
		return intentar;
	}

	private static List<RangeExtraction> comprobacionDeArgumentos(String auxiliar) {
		List<RangeExtraction> numbers = new ArrayList<>();
		String[] arguments = auxiliar.split(",");
		for (String string : arguments) {
			if(string.contains("-")) {
				String[] listaNumeros = string.split("-");
				//El programa no permite rango multiple al menos que este separado por una coma
				if(listaNumeros.length != 2) {
					System.out.println("The bookmark or page introduced is not a number");
					System.exit(1);
				}
				try {
					int numeroInicial = Integer.parseInt(listaNumeros[0]);
					int numeroFinal = Integer.parseInt(listaNumeros[1]);
					if(numeroFinal < numeroInicial) {
						System.out.println("The bookmark or page introduced is not a number");
						System.exit(1);
					}
					RangeExtraction rango = new RangeExtraction(numeroInicial,numeroFinal);
					numbers.add(rango);
				}catch(java.lang.NumberFormatException e) {
					System.out.println("The bookmark or page introduced is not a number");
					System.exit(1);
				}
			}
			else {
				try {
					int numeroInicial = Integer.parseInt(string);
					RangeExtraction rango = new RangeExtraction(numeroInicial);
					numbers.add(rango);
				}catch(java.lang.NumberFormatException e) {
					System.out.println("The bookmark or page introduced is not a number");
					System.exit(1);
				}
			}
		}
		return numbers;
	}

	public void getAllFiles(File dir, List<File> fileList) {

		File[] files = dir.listFiles();
		for (File file : files) {
			fileList.add(file);
			if (file.isDirectory()) {
				getAllFiles(file, fileList);
			} 
		}
	}

	public String writeZipFile(File directoryToZip, List<File> fileList) {
		String rutaOutputFolder = env.getProperty("crawly.paths.pdfFiles") + "//";
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(rutaOutputFolder + directoryToZip.getName() + ".zip");
			ZipOutputStream zos = new ZipOutputStream(fos);
			for (File file : fileList) {
				if (!file.isDirectory()) { // we only zip files, not directories
					addToZip(directoryToZip, file, zos);
				}
			}
			zos.close();
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return directoryToZip.getName();
	}

	public void addToZip(File directoryToZip, File file, ZipOutputStream zos) throws FileNotFoundException,
	IOException {

		FileInputStream fis = new FileInputStream(file);

		// we want the zipEntry's path to be a relative path that is relative
		// to the directory being zipped, so chop off the rest of the path
		String zipFilePath = file.getCanonicalPath().substring(directoryToZip.getCanonicalPath().length() + 1,
				file.getCanonicalPath().length());
		ZipEntry zipEntry = new ZipEntry(zipFilePath);
		zos.putNextEntry(zipEntry);
		byte[] bytes = new byte[1024];
		int length;
		while ((length = fis.read(bytes)) >= 0) {
			zos.write(bytes, 0, length);
		}
		zos.closeEntry();
		fis.close();
	} 

	private static byte[] readContentIntoByteArray(File file)
	{
		FileInputStream fileInputStream = null;
		byte[] bFile = new byte[(int) file.length()];
		try
		{
			//convert file into array of bytes
			fileInputStream = new FileInputStream(file);
			fileInputStream.read(bFile);
			fileInputStream.close();

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return bFile;
	}
}

