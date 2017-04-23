import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;

public class PhysLayerClient {
	public enum Signal {UP, DOWN}

	public static void main(String args[]) throws IOException{
		byte[] bitStorage = new byte[320];
		byte[] newBitStorage;
		byte[] origMsg;

		try (Socket socket = new Socket("codebank.xyz", 38002)){
			System.out.println("Connected to: " + socket.getInetAddress() + ":" + socket.getPort() + "\n");
			OutputStream os = socket.getOutputStream();
			InputStream is = socket.getInputStream();

			double baseline = 0;
			for (int i = 0; i < 64; ++i){
				baseline += is.read();
			}
			System.out.println("Baseline: " + (baseline /= 64));

			get5BNRZI(is, bitStorage, baseline);
			System.out.println();
			newBitStorage = decodeNRZI(bitStorage);
			
			convert5B4B(newBitStorage);
			origMsg = halfArray(newBitStorage);	
			os.write(origMsg);
			
			System.out.println(is.read());

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static void get5BNRZI(InputStream is, byte[] bitStorage, double baseline) throws IOException {
		byte receiver = 0;
		System.out.println("start: ");

		for (int i = 0; i < 320; ++i){ 			//320
			receiver = (byte) is.read();
			if ((receiver & 0xFF) > baseline){
				bitStorage[i] = (byte) 0x01;
			}else if ((receiver & 0xFF) < baseline){
				bitStorage[i] = (byte) 0x00;
			}
			//			System.out.println("receiver: " + (receiver & 0xFF) + ", baseline: " + (int) baseline + " result: " + bitStorage[i]);
			System.out.print(bitStorage[i] & 0x1);
		}
	}

	//return 64bytes of 5bits
	public static byte[] decodeNRZI(byte[] bitStorage){
		Signal sign = Signal.DOWN;
		byte[] result = new byte[64];
		byte decoded = 0x00;

		System.out.println("result: ");
		for (int i = 0; i < 64; ++i){ 			//64
			decoded &= 0x00;
			//				System.out.print("\n" + bitStorage[5*i] + " " + bitStorage[5*i + 1] + " " + bitStorage[5*i + 2] + " " + bitStorage[5*i + 3] + " " + bitStorage[5*i + 4]);
			
			// if prev signal is down and new signal is up, flip.
			//iter 1
			if (((bitStorage[5*i] & 0x01)  == 1 && sign == Signal.DOWN) 	
				|| ((bitStorage[5*i] & 0x01) == 0 && sign == Signal.UP)){
				decoded |= 0x10;
//				System.out.print(i + ": " + sign);
				if (sign == Signal.DOWN){
					sign = Signal.UP;
				}
				else{
					sign = Signal.DOWN;
				}
//				System.out.println(" " + Integer.toBinaryString(decoded & 0x1F) + "signal: " + sign);
			}

			//iter 2
			if(((bitStorage[(5*i) + 1] & 0x01) == 0 && sign == Signal.UP)
					|| ((bitStorage[(5*i) + 1] & 0x01) == 1 && sign == Signal.DOWN)){
				decoded |= 0x08;
//				System.out.print(i + ": " + sign);
				if (sign == Signal.DOWN){
					sign = Signal.UP;
				}
				else{
					sign = Signal.DOWN;
				}
//				System.out.println(i + ": " + Integer.toBinaryString(decoded & 0x1F) + "signal: " + sign);
			}

			//iter 3
			if(((bitStorage[(5*i) +2] & 0x01) == 0x00 && sign == Signal.UP)
					|| ((bitStorage[(5*i) +2] & 0x01) == 1 && sign == Signal.DOWN)){
				decoded |= 0x04;
//				System.out.print(i + ": " + sign);
				if (sign == Signal.DOWN){
					sign = Signal.UP;
				}
				else{
					sign = Signal.DOWN;
				}
//				System.out.println(i + ": " + Integer.toBinaryString(decoded & 0x1F) + "signal: " + sign);
			}

			//iter 4
			if(((bitStorage[(5*i) +3] & 0x01) == 0x00 && sign == Signal.UP)
					|| ((bitStorage[(5*i) +3] & 0x01) == 1 && sign == Signal.DOWN)){
				decoded |= 0x02;
//				System.out.print(i + ": " + sign);
				if (sign == Signal.DOWN){
					sign = Signal.UP;
				}
				else{
					sign = Signal.DOWN;
				}
//				System.out.println(i + ": " + Integer.toBinaryString(decoded & 0x1F) + "signal: " + sign);
			}

			//iter 5
			if(((bitStorage[(5*i)+4] & 0x01) == 0x00 && (sign == Signal.UP))
					|| ((bitStorage[(5*i)+4] & 0x01) == 1 && sign == Signal.DOWN)){
				decoded |= 0x01;
//				System.out.print(i + ": " + sign);
				if (sign == Signal.DOWN){
					sign = Signal.UP;
				}
				else{
					sign = Signal.DOWN;
				}
//				System.out.println(i + ": " + Integer.toBinaryString(decoded & 0x1F) + "signal: " + sign);
			}
			result[i] = decoded;
			//System.out.println("\nresult: " + Integer.toBinaryString(result[i] & 0x1F));
			//			System.out.println("\nresult: " + (result[i] & 0x1F));
			//	System.out.println(Integer.toBinaryString(result[i] & 0x1F));
		}
		return result;
	}

	public static byte[] halfArray(byte[] bitStorage){
		byte[] halfArray = new byte[bitStorage.length/2];
		byte combine = 0;
		for (int i = 0; i < bitStorage.length/2; i++){
			combine = bitStorage[2*i] <<= 4;
			combine &= 0xF0;
			combine = (byte) (combine | bitStorage[2*i+1]);
			halfArray[i] = (byte) (bitStorage[2*i] | bitStorage[2*i+1]);
//			System.out.println(i + ": " + (halfArray[i] & 0xFF));
		}
		return halfArray;
	}
	public static void convert5B4B(byte[] bitStorage){
		/*		byte fiveBitTable[] = {30, 9, 20, 21, 10, 11, 14, 15,
				18, 19, 22, 23, 26, 27, 28, 29};*/
		HashMap<Integer, Integer> fiveBitToFourBit = new HashMap<Integer ,Integer>(){{
			put(30, 0);
			put(9, 1);
			put(20, 2);
			put(21, 3);
			put(10, 4);
			put(11, 5);
			put(14, 6);
			put(15, 7);
			put(18, 8);
			put(19, 9);
			put(22, 10);
			put(23, 11);
			put(26, 12);
			put(27, 13);
			put(28, 14);
			put(29, 15);
		}};
		int temp;
		for (int i = 0; i < bitStorage.length; ++i){
			temp = fiveBitToFourBit.get(bitStorage[i] & 0x1F);
			bitStorage[i] = (byte) (temp & 0xF);
			//			System.out.println(bitStorage[i] & 0xF);
		}
	}
}

/*	public static void get5BNRZI(InputStream is, byte[] bitStorage, double baseline) throws IOException {
byte receiver = 0, hold = 0;
int arrSlot = 0;

for (int i = 0; i < 1; ++i){ //320/5 = 64
	receiver = (byte) is.read();	//get 8 bits
	System.out.println("orig msg: " + (Integer.toBinaryString(receiver & 0xFF)));
	//1
	bitStorage[arrSlot++] = (byte) (receiver >> 3);	//inc arrSlot, store left 5 values into bitStorage
	//				System.out.println("storing:  " + Integer.toBinaryString(bitStorage[i*8] & 0x1F));
	receiver &= 0x7; 		//keep 3 right values
	//System.out.println("r after: " + (Integer.toBinaryString(receiver & 0xFF)));		
	hold = receiver; 		// hold leftover 3 bits
	hold <<= 2;				// Make room for remaining 2 bits on right

	receiver = (byte) is.read();
	//				System.out.println("orig msg: " + (Integer.toBinaryString(receiver & 0xFF)));
	hold = (byte) (hold | (receiver >> 6 & 0x3)); // give hold 2 bits
	receiver &= 0x7F; //(127) keep 6 right values
	//2
	bitStorage[arrSlot++] = hold;
	//				System.out.println("storing:  " + Integer.toBinaryString(bitStorage[i*8+1] & 0x1F));
	//3
	bitStorage[arrSlot++] = (byte) (receiver >> 1 & 0x1F); 	//0001 1111
	//				System.out.println("storing:  " + Integer.toBinaryString(bitStorage[i*8+2] & 0x1F));
	receiver &= 0x1;// 1 bit left

	hold = receiver; // has 1 bit
	hold <<= 4;
	receiver = (byte) is.read();
	//				System.out.println("orig msg: " + (Integer.toBinaryString(receiver & 0xFF)));
	hold = (byte) (hold | (receiver >> 4 & 0xF)); // give hold 4 bits

	//4
	bitStorage[arrSlot++] = hold;
	//				System.out.println("storing:  " + Integer.toBinaryString(bitStorage[i*8+3] & 0x1F));
	receiver &= 0xF; 

	hold = receiver; //4 bit hold
	receiver = (byte) is.read();
	//				System.out.println("orig msg: " + (Integer.toBinaryString(receiver & 0xFF)));
	hold <<= 1;
	hold = (byte) (hold | receiver >> 7 & 0x1); //give hold 1 bit
	receiver &= 0x7F;// receiver has 7 bits
	//5
	bitStorage[arrSlot++] = hold;
	//				System.out.println("storing:  " + Integer.toBinaryString(bitStorage[i*8+4] & 0x1F));
	//6
	bitStorage[arrSlot++] = (byte) (receiver >> 2 & 0x1F);
	//				System.out.println("storing:  " + Integer.toBinaryString(bitStorage[i*8+5] & 0x1F));
	receiver &= 0x3; //keep 2 bits 

	hold = receiver; // has 2 bits
	hold <<= 3;
	receiver = (byte) is.read();
	//				System.out.println("orig msg: " + (Integer.toBinaryString(receiver & 0xFF)));
	hold = (byte) (hold | (receiver >> 5 & 0x7));
	//7 
	bitStorage[arrSlot++] = hold;
	//				System.out.println("storing:  " + Integer.toBinaryString(bitStorage[i*8+6] & 0x1F));
	receiver &= 0x1F;
	//8
	bitStorage[arrSlot++] = receiver;
	//				System.out.println("storing:  " + Integer.toBinaryString(bitStorage[i*8+7] & 0x1F));

}
}*/
/*	public static void decodeNRZI(byte[] bitStorage){
		Signal sign; //= Signal.DOWN; // represent prev bit 
		byte decoded;

		for (int i = 0; i < 5; ++i){
			sign = Signal.DOWN;
			System.out.println("Stored Value " + i + ":" + Integer.toBinaryString(bitStorage[i] & 0x1F));
			decoded = 0;

			//iter 1
			if ((bitStorage[i] >> 4 & 0x1) == 0x1 && sign == Signal.DOWN){	//flip
				sign = Signal.UP;
				decoded |= 0x10;
			}
			else if((bitStorage[i] >> 4 & 0x1) == 0 && sign == Signal.UP){ //flip
				sign = Signal.DOWN;
				decoded |= 0x10;
			}
			bitStorage[i] &= 0xF; // rightmost 4 bits to decode

			//iter 2
			if(((bitStorage[i] >> 3 & 0x1) == 0 && sign == Signal.UP)
					|| ((bitStorage[i] >> 3 & 0x1) == 0x1 && sign == Signal.DOWN)){
				decoded |= 0x8;
				if (sign == Signal.DOWN){
					sign = Signal.UP;
				}
				else{
					sign = Signal.DOWN;
				}
			}
			bitStorage[i] &= 0x07;	// rightmost 3 bits

			//iter 3
			if(((bitStorage[i] >> 2 & 0x1) == 0 && sign == Signal.UP)
					|| ((bitStorage[i] >> 2 & 0x1) == 1 && sign == Signal.DOWN)){
				decoded |= 0x4;
				if (sign == Signal.DOWN){
					sign = Signal.UP;
				}
				else{
					sign = Signal.DOWN;
				}
			}
			bitStorage[i] &= 0x03;

			//iter 4
			if(((bitStorage[i] >> 1 & 0x1) == 0 && sign == Signal.UP)
					|| ((bitStorage[i] >> 1 & 0x1) == 1 && sign == Signal.DOWN)){
				decoded |= 0x2;
				if (sign == Signal.DOWN){
					sign = Signal.UP;
				}
				else{
					sign = Signal.DOWN;
				}
			}
			bitStorage[i] &= 0x01;

			//iter 5
			if(((bitStorage[i] & 0x1) == 0 && sign == Signal.UP)
					|| ((bitStorage[i] & 0x1) == 1 && sign == Signal.DOWN)){
				decoded |= 0x1;
				if (sign == Signal.DOWN){
					sign = Signal.UP;
				}
				else{
					sign = Signal.DOWN;
				}
			}
			bitStorage[i] = decoded;
			System.out.println("NRZI decoded:  " + (Integer.toBinaryString(bitStorage[i] & 0x1F)));
		}
	}*/

/*	public static void get5BNRZI(InputStream is, byte[] bitStorage) throws IOException {
		int digits = 0, arrSlot = 0;
		short fiveBitStorage = 0;
		byte received = 0;

		for (int j = 0; j < 5; ++j){	// incoming 320bytes
			received = (byte) is.read();
			fiveBitStorage = (short) (fiveBitStorage | received);
	//		System.out.println(Integer.toBinaryString(fiveBitStorage & 0xFF) + "  " + Integer.toBinaryString(received & 0xFF));
			digits += 8;

			while (digits / 5 > 0){	// never more than 4 bits long after loop, shift remainder to left 8 bits
	//			System.out.println("digits : " + digits);

				bitStorage[arrSlot++] = (byte) (fiveBitStorage >> (digits - 5));	// get the five bit representation of the byte to be converted

	//			System.out.println("storing: " +  Integer.toBinaryString(bitStorage[arrSlot] & 0x1F));
				System.out.println(fiveBitStorage & 0xFF);
				if (fiveBitStorage > 15 && fiveBitStorage < 65536){		// 1 0000 - 0111 1111 1111 1111
					fiveBitStorage <<= (20-(digits));
					System.out.println("in");
				}
				else if (fiveBitStorage < 16 && fiveBitStorage >= 0){	// 0 - 1111  
					fiveBitStorage <<= (15-digits);
					System.out.println("out");
				}
				digits -= 5;
			}
		}
	}*/


/*				if (fivebitStorage == 0){// gets 2^exponent, 0-7
					digits = 0;
				}
				else{
					digits = (int) Math.round((Math.log10(decoder)/Math.log10(2))); 
				}

				if (digits > 3){
					for (int k = digits; k > digits-5; --k){

					}
				}*/

/*				if (decoder == 0){
					decoder = (byte) is.read();
				}
				else if(decoder > 255){
					decoder = (short) (decoder | is.read());
				}
				else{
//					digits = (int) Math.round((Math.log10(decoder)/Math.log10(2)));
					decoder <<= 8;
				}*/
//				System.out.println("count: " + ++count + " byte: " + temp);

