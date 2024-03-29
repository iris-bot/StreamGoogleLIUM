/**
 * @author pedro@iris-bot.com.br
 *
 */
package br.com.irisbot.asr.speaker;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFileFormat.Type;

public class DiarizationActorSupplier implements Supplier<String> {
	private static String response = "";
	private static boolean isOutputClosed = false;
	private static List<String> params;
	// private static String fileName = "";
	private static boolean semaphore = true;
	public DiarizationActorSupplier(ByteArrayOutputStream out, File file, AudioFormat format) {
		getAudioFile(out, file, format);
	}
	/**
	 * Write bytes from outputstream to file, ignore if irrelevant (too small)
	 * Call function that identify speakers
	 * @param out 		content of file
	 * @param file 		physical file
	 * @param format	format expected it has
	 */
	public void getAudioFile(ByteArrayOutputStream out, File file, AudioFormat format) {
		if (!isOutputClosed) {
			byte[] audio = out.toByteArray();
			InputStream input = new ByteArrayInputStream(audio);
			try {
				// Tiny files are ignored
				if (audio.length >= 2048) {
					final AudioInputStream ais = new AudioInputStream(input, format,
							audio.length / format.getFrameSize());
					// New sample rate 16000
					AudioFormat newFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000, 16, 1, 2, 8000,
							false);
					AudioInputStream highResAIS = AudioSystem.getAudioInputStream(newFormat, ais);
					AudioSystem.write(highResAIS, Type.WAVE, file);

					ais.close();
					//Small files are also ignore because usually is just a bump
					if (file.length() < 32052) {
						file.delete();
					} else {
						GuessWho(file.getName());
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	/**
	 * Call LIUM diarization
	 * @param filename	name of the file that was input
	 */
	public void GuessWho(String filename) {
		if (semaphore) {
			semaphore=false;
			new Thread(new Runnable() {
				@Override
				public void run() {
					//TODO change cmd.exe to /bin/sh with flag -c to linux-based systems
					params = Arrays.asList("cmd.exe", "/C", "go.sh");
					executeCommand(params, true);
					
					//SH before command bellow make response return
					params = Arrays.asList("cmd.exe", "/C", "sh results.sh " + filename.split("\\.")[0]);
					response = executeCommand(params, true);
					semaphore=true;
				}
			}).start();
		}
		
		
	}
	/**
	 * Control over the output on Stream.java that also send  the voice to google
	 * @return
	 */
	public boolean isClosed() {
		return isOutputClosed;
	}
	public static void setClosed(boolean flag) {
		isOutputClosed = flag;
		//delete temp files
		params = Arrays.asList("cmd.exe", "/C", "rm -rf tmp/input/*");
		executeCommand(params, false);
		
		params = Arrays.asList("cmd.exe", "/C", "rm -rf tmp/output/*");
		executeCommand(params, false);
	}
	
	/**
	 * Execute shell commands
	 * @param List 		First the command, then arguments
	 * @param boolean	If we want a response
	 * @return
	 */
	public static String executeCommand(List<String> params, boolean waitForResponse) {
		 
		String response = "";
		 
		ProcessBuilder pb = new ProcessBuilder(params);
		pb.redirectErrorStream(true);
		pb.directory(new File("C://var//www//SpeechRecognitionService"));
		
		 
		try {
			Process shell = pb.start();
			 
			if (waitForResponse) {
			 
				// To capture output from the shell
				InputStream shellIn = shell.getInputStream();
				 
				// Wait for the shell to finish and get the return code
				int shellExitStatus = shell.waitFor();
				if (shellExitStatus > 0) {
					System.out.println("Exit status  " + shellExitStatus);
					response = "";
				} else {
					response = convertStreamToStr(shellIn);
					//System.out.println(response);
				}
				
				shellIn.close();
			}
		 
		} catch (IOException e) {
			System.out.println("Error occured while executing Linux command. Error Description: "
					+ e.getMessage());
		} catch (InterruptedException e) {
			System.out.println("Error occured while executing Linux command. Error Description: "
			+ e.getMessage());
		}
		 
		return response;
	}
		 
	/*
	* To convert the InputStream to String, use the Reader.read(char[]
	* buffer) method. Iterate until the Reader return -1 which means
	* there's no more data to read.
	*/
	public static String convertStreamToStr(InputStream is) throws IOException {
	 
		if (is != null) {
			Writer writer = new StringWriter();
			 
			char[] buffer = new char[1024];
			try {
				Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
					
				}
			} finally {
				is.close();
			}
			return writer.toString();
		}
		else {
			return "";
		}
	}
	/**
	 * From Supplier super-class
	 */
	@Override
	public String get() {
		return response;
	}

}