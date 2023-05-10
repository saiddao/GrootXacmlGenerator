package it.unipi.di.lai.prova;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.google.common.io.Files;

public class ProvaFile {

	public ProvaFile() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			File tmpDir = Files.createTempDir();
			System.out.println(tmpDir.getAbsolutePath());
			
			File tmpFile = File.createTempFile("pippo_", ".xml", tmpDir);
			System.out.println(tmpFile.getAbsolutePath());
			copyContent(tmpFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void copyContent(File tmpFile) {
		// TODO Auto-generated method stub
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(tmpFile);
			fos.write("Hello World".getBytes());
		} catch (FileNotFoundException e) {
			System.out.println("Errore nell'apertura del file: " + e);
		} catch (IOException e) {
			System.out.println("Errore nella scrittura del file: " + e);
		} finally {
			try { fos.close(); } catch (Exception e) {}
		}
	}

}
