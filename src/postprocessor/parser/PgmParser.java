package postprocessor.parser;
import  postprocessor.color.Color32;
import  postprocessor.color.Color32.UnknownFormat;
import  postprocessor.color.Color32Surface;
import  postprocessor.color.Color32Surface.FORMAT;
import postprocessor.color.Layer32;
import postprocessor.parser.PamParser.PamHeader;
import  postprocessor.parser.PnmParser.PNMTYPE;
import  postprocessor.struct.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;

import com.sun.prism.Image;



public class PgmParser extends PnmParser
{  
	public static class PgmHeader 
	{
		public PNMTYPE type;
		public FORMAT  color;
		public @Struct.Point long size;
		public int     maxval;
		public int     stride;
		
		public PgmHeader( @Struct.Point long imgSize, PNMTYPE type ) throws UnknownFormat {
			this.type = type; maxval = 64535; color = Color32Surface.SURFACECODE.fromChannelOrder("ggaa").format;
			size = imgSize; stride = Struct.getX(size)*2;
		} 
		public byte[] asRawData() {
			StringBuffer buf = new StringBuffer();
			buf.append(type.toString()); // Magic
			buf.append('\n'); // Width
			buf.append(Integer.toString(Struct.getX(size)));
			buf.append(' ');  // Height
			buf.append(Integer.toString(Struct.getY(size)));
			buf.append('\n'); // Maxval
			buf.append(Integer.toString(maxval));
			buf.append('\n');
			return buf.toString().getBytes(Charset.forName("ASCII"));
		}
		public int bytes() {
			return (stride*Struct.getY(size))*2; 
		}
		public PamHeader toPamHeader() {
			return PamHeader.fromPnmType( type, Struct.getX(size), Struct.getY(size) ); 
		}
	};

	private int proceedPPMline(int pixelsCount, String line, ImageData image)
			throws ParseException {
		if (!line.startsWith("#")) {
			String[] numbers = line.split(" ");
			int channel = 0; short alpha = m_color.getAlpha();
			for ( int i = 0; i < numbers.length; ++i ) {
			    channel = Short.parseShort(numbers[i].trim());  
				int xPos = pixelsCount % image.width;
				int yPos = pixelsCount / image.width;
				image.setPixel(xPos, yPos, channel);
				image.setAlpha( xPos, yPos, alpha );
				pixelsCount++;
			}
		}return pixelsCount;
	}
	
	protected String getNextLine( FileInputStream input, StringBuffer buffer )
	{ try { int c = 0; 
			while((c = input.read())!=-1)
				if(c=='\n')	break;
				else buffer.append((char)c);
		} catch (IOException e) {
			System.err.printf(e.getMessage());
		} if(buffer.charAt(0) != '#')
			 return buffer.toString();
		else return getNextLine( input, buffer ); }
	
	protected int getNextRow( FileInputStream input, byte[] data, PgmHeader header )
	{ try { int c = header.stride; 
		    return input.read(data,0,c);
	    } catch (IOException e) {
			System.err.printf(e.getMessage());
			return 0;
		}  
	}
	
	protected PgmHeader parseHeader(FileInputStream input) throws ParseException, IOException, Exception
	{
		StringBuffer buffer = new StringBuffer();
		PNMTYPE magic;
        
		String line = getNextLine( input, buffer );
			
		if( line.contains( "P2" ) )
			magic = PNMTYPE.P2;
		else if( line.contains( "P5" ) )
			magic = PNMTYPE.P5;
		else {
			System.err.println( "file is neither 'P2' nor 'P5' image" );
			throw new ParseException( "wrong or unknown magic", 1 );
		} int search = 0; int[] parameters = new int[3];
		do { line = getNextLine( input, buffer );
			String[] numbers = line.split(" ");
		for(int i=0; i<numbers.length; ++i) {
			parameters[search++] = Integer.parseUnsignedInt(numbers[i]);
			if( search == 3 ) break; }
		} while(search <= 2);
		PgmHeader hdr = new PgmHeader( Struct.newPoint( parameters[0], parameters[1] ), magic);
        return hdr;
	}

	public PgmParser( FORMAT outputformat ) {
		super( PNMTYPE.P2, outputformat );	
		m_color = new Color32.GRAY();
		m_color.setG(32767).setA(65535);
	}

	@Override
	public ImageData loadImage( String fileName ) throws Exception {
		FileInputStream file = new FileInputStream(fileName);
		PgmHeader header = parseHeader(file);
		PaletteData paletti = new PaletteData( 0xffff00, 0xffff00, 0xffff00 );
		ImageData dest = new ImageData( Struct.getX(header.size), 
				                        Struct.getY(header.size),
				                        24, paletti );
		if(header.type == PNMTYPE.P2)
		{  
			int count = 0;
			StringBuffer buffer = new StringBuffer();
			do { count = proceedPPMline(count,getNextLine(file,buffer),dest);
			} while( count <= (header.bytes()/2) );
		} 
		if(header.type == PNMTYPE.P5)
		{
			byte[] rowdata = new byte[header.stride];
			int counteins = header.bytes();
			int countzwei = 0;
			while(counteins>0) {
			    int row = getNextRow(file,rowdata,header);
			    for(int i=0; i<row; i+=2) {
				    m_color.setG((short)(rowdata[i]|rowdata[i+1]<<8));
				    dest.setPixel( i/2, countzwei, m_color.getG() );
				    dest.setAlpha( i/2, countzwei, m_color.getAlpha() );
			    } counteins -= row;
			    ++countzwei;
			}
		}
		return dest;
	}
	
	public Layer32 loadLayer(String fileName) throws Exception
	{
		FileInputStream f = new FileInputStream(fileName);
		PgmHeader pgm = parseHeader( f );
		PamHeader pam = pgm.toPamHeader();
	    PamParser forwardparser = new PamParser( m_color.format() );
	    forwardparser.defaultBackgroundColor().assign( m_color );
	    return forwardparser.parseLayerFromInputStream( f, pam );
	}
}
