package step.grid.filemanager;

import java.io.IOException;

public interface FileProvider {
	
	public TransportableFile getTransportableFile(String fileHandle) throws IOException;
	
	public static class TransportableFile {
		
		protected boolean isDirectory;
		
		protected byte[] bytes;

		public TransportableFile(boolean isDirectory, byte[] bytes) {
			super();
			this.isDirectory = isDirectory;
			this.bytes = bytes;
		}

		public boolean isDirectory() {
			return isDirectory;
		}

		public byte[] getBytes() {
			return bytes;
		}
	}
}
