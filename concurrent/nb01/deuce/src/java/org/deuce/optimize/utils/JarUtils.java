package org.deuce.optimize.utils;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class JarUtils {

	public JarUtils() {

	}

	public Manifest extractJar(String jarFilename, String destDir)
			throws IOException {
		return extractJarInternal(jarFilename, destDir);
	}

	private Manifest extractJarInternal(String jarFile, String destDir)
			throws IOException {
		System.out.println(String.format("Extracting %s to %s...", jarFile,
				destDir));
		JarFile jar = new JarFile(jarFile);
		Manifest manifest = jar.getManifest();
		Enumeration<? extends JarEntry> enumeration = jar.entries();
		while (enumeration.hasMoreElements()) {
			JarEntry file = enumeration.nextElement();
			if (file.isDirectory())
				continue;
			java.io.File f = new java.io.File(destDir + java.io.File.separator
					+ file.getName());
			new File(f.getParent()).mkdirs();
			java.io.InputStream is = jar.getInputStream(file); // get the input
			// stream
			java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
			byte[] b = new byte[4096];
			int read = 0;
			read = is.read(b);
			while (read >= 0) {
				fos.write(b, 0, read);
				read = is.read(b);
			}

			//			while (is.available() > 0) { // write contents of 'is' to 'fos'
			//				fos.write(is.read());
			//			}
			fos.close();
			is.close();
		}
		return manifest;
	}

	public void createJar(String dir, String jarFile, Manifest manifest)
			throws IOException {
		System.out.println(String.format("Zipping %s to %s...", dir, jarFile));
		JarOutputStream zos = new JarOutputStream(new java.io.FileOutputStream(
				jarFile), manifest);
		//assuming that there is a directory named inFolder (If there 
		//isn't create one) in the same directory as the one the code  runs from, 
		//call the zipDir method 
		createJarInternal(dir.length() + 1, dir, zos);
		//close the stream 
		zos.close();
	}

	public void createJarInternal(int dirPrefixLength, String dir2zip,
			JarOutputStream zos) throws IOException {
		//create a new File object based on the directory we have to zip File    
		File zipDir = new File(dir2zip);
		//get a listing of the directory content 
		String[] dirList = zipDir.list();
		byte[] readBuffer = new byte[2156];
		int bytesIn = 0;
		//loop through dirList, and zip the files 
		for (int i = 0; i < dirList.length; i++) {
			File f = new File(zipDir, dirList[i]);
			if (f.isDirectory()) {
				//if the File object is a directory, call this 
				//function again to add its content recursively 
				String filePath = f.getPath();
				createJarInternal(dirPrefixLength, filePath, zos);
				//loop again 
				continue;
			}
			//if we reached here, the File object f was not a directory 
			//create a FileInputStream on top of f 
			java.io.FileInputStream fis = new java.io.FileInputStream(f);
			// create a new zip entry 
			JarEntry anEntry = new JarEntry(f.getPath().substring(
					dirPrefixLength).replace("\\", "/"));
			if (anEntry.getName().equalsIgnoreCase("META-INF/MANIFEST.MF"))
				continue;
			//place the zip entry in the ZipOutputStream object 
			zos.putNextEntry(anEntry);
			//now write the content of the file to the ZipOutputStream 
			while ((bytesIn = fis.read(readBuffer)) != -1) {
				zos.write(readBuffer, 0, bytesIn);
			}
			//close the Stream 
			fis.close();
		}
	}

}
