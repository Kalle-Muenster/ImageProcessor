package postprocessor.parser;

import postprocessor.color.Color32;
import postprocessor.color.Color32.UnknownColor;
import postprocessor.color.Color32Surface;
import postprocessor.color.Color32Surface.*;
import postprocessor.color.Layer32;
import postprocessor.parser.PnmParser.PNMTYPE;
import postprocessor.struct.Struct;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;


//loading P7 or P8 format PAM (.pam) files:
public class PamParser extends PnmParser
{
	public enum TUPLTYPE
	{
		BLACKANDWHITE,
		GRAYSCALE,
		GRAYSCALE_ALPHA,
		RGB,
		RGB_ALPHA,
	}
	
	public static class PamPosition
	{
	    public int X;
	    public int Y;
	    public int C;
	}

	public static class PamHeader
	{
		public PNMTYPE FORMAT = PNMTYPE.P7;
		public int WIDTH = 1;
		public int HEIGHT = 1;
		public int DEPTH = 1;
		public int MAXVAL = 1;
		public TUPLTYPE[] TYPES = new TUPLTYPE[0];
		public String[] TUPELS = new String[0];
		
		private void defaultsByTupelType( TUPLTYPE type ) {
			DEPTH=type.compareTo( TUPLTYPE.BLACKANDWHITE );
			switch( type ) {
			case GRAYSCALE:
			case GRAYSCALE_ALPHA:
				MAXVAL = 65535;
				break;
			case RGB:
			case RGB_ALPHA:
				MAXVAL = 255;
				break;
			} TYPES = new TUPLTYPE[1];
			TYPES[0] = type;
		}
		
		public PamHeader() {}
		public PamHeader( int w, int h, int depth, int max, String[] types ) {
			WIDTH=w;
			HEIGHT=h;
			DEPTH=depth;
			MAXVAL = max;
			TUPELS = types;
		}
		public PamHeader( TUPLTYPE type, int w, int h ) {
			defaultsByTupelType( type );
			WIDTH=w; 
			HEIGHT=h;
		}
		public PamHeader( PNMTYPE magic, int w, int h ) {
			WIDTH=w;
			HEIGHT=h;
			TYPES = new TUPLTYPE[1];
			switch( magic ) {
				case P1:
				case P2:
				case P3: FORMAT = PNMTYPE.P8;
					break;
				case P4:
				case P5:
				case P6: FORMAT = PNMTYPE.P7;
			    	break;
			default: FORMAT = magic; }
			switch( magic ) {
				case P1: 
				case P4: TYPES[0] = TUPLTYPE.BLACKANDWHITE;
					break;
				case P2:
				case P5: TYPES[0] = TUPLTYPE.GRAYSCALE;
					break;
				case P3:
				case P6: TYPES[0] = TUPLTYPE.RGB;
			    	break;
			default:
				TYPES[0] = TUPLTYPE.RGB_ALPHA;
			}
			defaultsByTupelType( TYPES[0] );
		}
		
		public static PamHeader fromPnmType( PNMTYPE magic, int width, int height )
		{
			return new PamHeader( magic, width, height );
		}
		
		public static PamHeader fromTupelType( TUPLTYPE tupel, int width, int height )
		{
			return new PamHeader( tupel, width, height );
		}
		
		public byte[] toHeader() {
			StringBuffer hdr = new StringBuffer( FORMAT.toString() );
			hdr.append("\nWIDTH ");
			hdr.append(   WIDTH  );
			hdr.append("\nHEIGHT ");
			hdr.append(   HEIGHT  );
			hdr.append("\nDEPTH ");
			hdr.append(   DEPTH  );
			hdr.append("\nMAXVAL ");
			hdr.append(   MAXVAL  );
			if( TYPES.length >= TUPELS.length ) {
				for( int i=0; i<TYPES.length; ++i ) {
					hdr.append( "\nTUPLTYPE " );
					hdr.append( TYPES[i].toString() );
				}
			} else {
				for( int i=0; i<TUPELS.length; ++i ) {
					hdr.append( "\nTUPLTYPE " );
					hdr.append( TUPELS[i] );
				}
			} hdr.append( "\nENDHDR\n" );
			return hdr.toString().getBytes(
			        Charset.forName( "ASCII" )
			                            );
		}
	};
	
	public PamParser( Color32Surface.FORMAT outputformat )
	{
		super( PNMTYPE.P7, outputformat );
	}

	// parse one line of P8 format ASCII input
	private int proceedPAMline( PamPosition pos, String line,
			                    ImageData image, PamHeader hdr ) throws NumberFormatException
	{
		Color32 color = m_color.clone();
		int channels = hdr.DEPTH;
		int topchann = (channels - 1);
		topchann = topchann == 0 ? 1 : topchann;
		String[] numbers = line.split(" ");
		int[] channel = new int[4];
		channel[topchann] = 255;
		if( channels < 4 )
			channel[channels] = hdr.MAXVAL > 255
				              ? hdr.MAXVAL : 255;
		int count = 0;
		for( int i = 0; i < numbers.length; i += channels ) {
			for( int chan=0; chan<channels; ++chan ) {
				channel[chan] = Integer.parseUnsignedInt(
						            numbers[count++].trim()
					                                       );
				if( chan == topchann ) {
					if ( hdr.DEPTH < 3  ) {
						color.setRGB( channel[0] );
						color.setAlpha( channel[1] );
					} else {
						color.setR(channel[0]).setG(channel[1])
						     .setB(channel[2]).setAlpha(channel[3]);
					} image.setPixel( pos.X, pos.Y, color.getRGB() );
					image.setAlpha( pos.X, pos.Y, color.getAlpha() );
				}
			} if ( ++pos.X >= image.width ) {
				if ( ++pos.Y >= image.height ) break;
				pos.X = 0;
			}
		} return numbers.length / channels;
    }
	
	
	private int parseLineToLayer( PamPosition pos, String line,
                                  Layer32 layer, PamHeader hdr ) throws NumberFormatException
    {
        int channels = hdr.DEPTH;
        int topchann = (channels - 1);
        topchann = topchann == 0 ? 1 : topchann;
        String[] numbers = line.split(" ");
        int[] channel = new int[4];
        channel[topchann] = hdr.MAXVAL > 255 ? hdr.MAXVAL : 255;
        if( channels < 4 )
        	channel[channels] = channel[topchann];
        int count = 0;
        int width = layer.Width();
        int height = layer.Height();
        for ( int i = 0; i < numbers.length; i += channels ) {
        	for ( int chan = 0; chan < channels; ++chan ) {
        		channel[chan] = Integer.parseUnsignedInt(
        				         numbers[count++].trim() );
        		if( chan == topchann ) {
        			if ( hdr.DEPTH < 3  ) {
        				layer.setRGB( channel[0] );
        				layer.setAlpha( channel[1] );
        				//channel[3] = (channel[1]*255) / hdr.MAXVAL;
        				//channel[2] = (channel[0]*255) / hdr.MAXVAL;
        				//channel[0] = channel[1] = channel[2];
        			} else {
        				layer.setR(channel[0]).setG(channel[1])
        				     .setB(channel[2]).setAlpha(channel[3]);
        			} layer.setNext( layer.farb );
        		}
        	} if ( ++pos.X >= width ) {
        		if ( ++pos.Y >= height ) break;
        		pos.X = 0;
        	}
        } return numbers.length / channels;
    }
	
	
	private PamHeader parseHeader( FileInputStream reader ) throws ParseException, IOException, Exception
	{
		int c=0;
		PamHeader hdr = new PamHeader();
		StringBuffer buffer = new StringBuffer();

		while((c = reader.read())!=-1)
			if(c=='\n')	break;
			else buffer.append((char)c);
		
		String line = buffer.toString();
		if( line.contains("P7") )
			hdr.FORMAT = PNMTYPE.P7;
		else if( line.contains( "P8" ) )
			hdr.FORMAT = PNMTYPE.P8;
		else {
			System.err.println( "file is neither 'P7' nor 'P8' image" );
			throw new ParseException( "wrong or unknown magic number", 1 );
		}
		java.text.NumberFormat numberParser = java.text.NumberFormat.getInstance();
		List<String> tupels = new LinkedList<String>();
		line="#";
		while( !line.contains("ENDHDR") ) {
			while( line.startsWith("#") ) {
				buffer.setLength(0);
				while( (c=reader.read()) != -1) {
					if (c=='\n') break;
					else buffer.append((char)c);
				} line = buffer.toString();
			} String[] prop = line.split(" ");
			switch( prop[0].trim() ) {
				case "WIDTH": hdr.WIDTH = numberParser.parse(prop[1]).intValue(); line = "#"; break;
				case "HEIGHT": hdr.HEIGHT = numberParser.parse(prop[1]).intValue(); line = "#"; break;
				case "DEPTH": hdr.DEPTH = numberParser.parse(prop[1]).intValue(); line = "#"; break;
				case "MAXVAL": hdr.MAXVAL = numberParser.parse(prop[1]).intValue(); line = "#"; break;
				case "TUPLTYPE": tupels.add( prop[1].trim() ); line = "#"; break;
				case "ENDHDR": break;
				default: String error = String.format("unknown parameter in PAM header: '%s'",prop[0]);
						 System.err.println(error);
						 throw new Exception(error);
			}
		} hdr.TUPELS = new String[tupels.size()];
		for (int i=0;i<tupels.size(); ++i) {
			hdr.TUPELS[i] = tupels.get(i);
		} return hdr;
	}

	public Layer32 parseLayerFromInputStream( InputStream src, PamHeader pam ) throws IOException, NumberFormatException
	{
		StringBuffer buffer = new StringBuffer();
		java.text.NumberFormat numberParser = java.text.NumberFormat.getInstance();
		Layer32 dst = new Layer32( Struct.newPoint( pam.WIDTH, pam.HEIGHT ), FORMAT.BGRA );
		int c = 0; PamPosition pos = new PamPosition();
		pos.X = 0; pos.Y = 0; pos.C = 0;
		int PixelCount = (pam.WIDTH * pam.HEIGHT);
		/////////////////////////////////////////////////
		//int filesize = src.available();
		//byte[] filedata = new byte[filesize+4];
		//src.read( filedata, 0, filesize );
		
		//String restpadding = new String("0 0 0");
		//if(pam.DEPTH == 4) restpadding += " 0";
		
		/////////////////////////////////////////////////
		dst.letsGo( Layer32.Forwards );

	    // on 'P8' parse 'plain' (ASCII data)
		if ( pam.FORMAT == PNMTYPE.P8 )	{ 
			//String[] asciilines = new String(filedata,Charset.forName("ASCII")).split("\n");
			//filedata = null;
			//int currentLine = 0;
		    while( pos.Y < (pam.HEIGHT-1) && pos.C < PixelCount ) { 
				buffer.setLength(0);
				while( (c=src.read()) != -1 ) {
					if( c=='\n' ) break;
					else buffer.append( (char)c );
				} pos.C += parseLineToLayer(
					pos, buffer.toString(), dst, pam
		        );
			}
		} else { // on 'P7' parse 'binary' (byte or word data)
			Color32 color = m_color.clone();
			int[] channel = new int[]{ 
				m_color.getAlpha(), m_color.getAlpha(),
				m_color.getAlpha(), m_color.getAlpha()
			};
			int chanSize = (pam.MAXVAL<256?1:2);
			byte[] pixel = new byte[pam.DEPTH*chanSize];
			
			//int pixelsize = pixel.length;
			//int currentBP = 0;
			
			for( pos.Y = 0; pos.Y < pam.HEIGHT; ++pos.Y ) 
			{  
				for( pos.X = 0; pos.X < pam.WIDTH; ++pos.X ) {
					src.read(pixel);
					//if(currentBP < (filesize-pixelsize)) {
					//	for(int i = 0; i < pixelsize; ++i ) {
					//		pixel[i] = filedata[currentBP++];
					//	} 
					//} else {
					//	for(int i = 0; i < pixelsize; ++i ) {
					//		pixel[i] = 0;
					//	}
					//}
					if( chanSize == 2 ) {
						if( pam.DEPTH == 4 ) {
							channel[3] = ((pixel[6]|(pixel[7]<<8))*255)/pam.MAXVAL;
							color.setAlpha( channel[3] );
						} if (pam.DEPTH >= 3 ) {		
							channel[2] = ((pixel[4]|(pixel[5]<<8))*255)/pam.MAXVAL;
							channel[1] = ((pixel[2]|(pixel[3]<<8))*255)/pam.MAXVAL;
							channel[0] = ((pixel[0]|(pixel[1]<<8))*255)/pam.MAXVAL;
							color.setR( channel[0] ).setG( channel[1] ).setB( channel[2] );
						} else {
							// channel[3] = ((pixel[2]|(pixel[3]<<8))*255)/pam.MAXVAL;
							channel[0] = pixel[0] | (pixel[1]<<8);
                            channel[1] = pam.DEPTH == 2 ? (pixel[2] | (pixel[3]<<8)) : pam.MAXVAL;
							color.setRGB( channel[0] );
							color.setAlpha( channel[1] );
						}
					} else {
						for(int i=pam.DEPTH-1;i>=0;--i)
							channel[i] = (256+pixel[i])%256;
					    if ( pam.DEPTH < 3 ) {
						    channel[2] = channel[1] = channel[0];
					    } color.setR(channel[0]).setG(channel[1])
						       .setB(channel[2]).setA(channel[3]);
					} dst.setNext( color );
				}
			} //filedata = null;
		}
		return dst;
	}
	
	public ImageData parseFromInputStream( InputStream src, PamHeader pam ) throws IOException, ParseException 
	{
		StringBuffer buffer = new StringBuffer();
		java.text.NumberFormat numberParser = java.text.NumberFormat.getInstance();
		PaletteData paletti = new PaletteData( 0xff0000, 0x00ff00, 0x0000ff );
		ImageData dst = new ImageData( pam.WIDTH, pam.HEIGHT, 24, paletti );
		dst.alphaData = new byte[ pam.WIDTH * pam.HEIGHT ];
		int c = 0; PamPosition pos = new PamPosition();
		pos.X = 0; pos.Y = 0; pos.C = 0;
		int PixelCount = (pam.WIDTH * pam.HEIGHT);
		//////////////////////////////////////////////
		//int filesize = src.available();
		//byte[] filedata = new byte[filesize];
		//src.read(filedata);
		/////////////////////////////////////////////
		// on 'P8' parse 'plain' (ASCII data)
		if ( pam.FORMAT == PNMTYPE.P8 )	{
			////////////////////////////////////////////////////////////////////////////////
			//String[] asciilines = new String(filedata,Charset.forName("ASCII")).split("\n");
			//int currentLine = 0;
			////////////////////////////////////////////////////////////////////////////////
		    while( pos.Y < (pam.HEIGHT-1) && pos.C < PixelCount ) {
				buffer.setLength(0);
				while( (c=src.read()) != -1 ) {
					if( c=='\n' ) break;
					else buffer.append( (char)c );
				}
		    	pos.C += proceedPAMline(
		    		pos, buffer.toString(), dst, pam
			    );
			}
		} else { // on 'P7' parse 'binary' (byte or word data)
			Color32 color = m_color.clone();
			int[] channel = new int[]{ 
				m_color.getAlpha(), m_color.getAlpha(),
				m_color.getAlpha(), m_color.getAlpha()
			};
			int chanSize = (pam.MAXVAL<256?1:2);
			byte[] pixel = new byte[pam.DEPTH*chanSize];
			
			/////////////////////////////////////////////
			//int pixelsize = pixel.length;
			//int currentBP = 0;
			/////////////////////////////////////////////
			for( pos.Y = 0; pos.Y < pam.HEIGHT; ++pos.Y ) 
			{  
				for( pos.X = 0; pos.X < pam.WIDTH; ++pos.X ) {
					src.read(pixel);
					//for( int i=0; i<pixel.length; ++i ) {
					//	pixel[i] = filedata[currentBP++];
					//}
					if( chanSize == 2 ) {
						channel[0] = ((pixel[0]|(pixel[1]<<8))*255)/pam.MAXVAL;
						if( pam.DEPTH == 4 ) {
							channel[3] = ((pixel[6]|(pixel[7]<<8))*255)/pam.MAXVAL;
						} else if (pam.DEPTH > 2 ) {		
							channel[2] = ((pixel[4]|(pixel[5]<<8))*255)/pam.MAXVAL;
							channel[1] = ((pixel[2]|(pixel[3]<<8))*255)/pam.MAXVAL;
						} else
							channel[3] = ((pixel[2]|(pixel[3]<<8))*255)/pam.MAXVAL;
					} else {
						for(int i=pam.DEPTH-1;i>=0;--i)
							channel[i] = (256+pixel[i])%256;
					} if ( pam.DEPTH < 3 ) {
						channel[2] = channel[1] = channel[0];
					} dst.setPixel( pos.X, pos.Y, 
					  color.setR(channel[0]).setG(channel[1]).setB(channel[2]).getRGB()
							        );
					dst.alphaData[pos.X+pos.Y*pam.WIDTH] = (byte)channel[3];
				}
			}
		} return dst;
	}

    public ImageData loadImage( String fileName ) throws Exception
    {
    	ImageData loade = null;
    	FileInputStream f = null;
    	PamHeader hdr = null;
    	int yPos = 0;
    	try	{
    		f = new FileInputStream(fileName);
    		hdr = parseHeader(f);
    	} catch ( Exception Ex ) {
    		f.close();
    		loade = null;
    		System.err.println (
    			Ex.getMessage() + String.format(" at yPos %s",yPos)
    			                 );
    	} if( hdr == null )
    		return loade;
    	
    	m_type  = hdr.FORMAT;
    	if( hdr.TYPES.length == 0 ) {
    		hdr.TYPES = new TUPLTYPE[1];
    	} hdr.TYPES[0] = TUPLTYPE.valueOf( hdr.TUPELS[0] );
    		
    	loade = parseFromInputStream( f, hdr );
    	f.close();
    	return loade;
    }
    
    public Layer32 loadLayer( String fileName ) throws Exception
    {
    	Layer32 loade = null;
    	PamHeader hdr = null;
    	FileInputStream f = null;
    	int yPos = 0;
    	try	{
    		f = new FileInputStream(fileName);
    		hdr = parseHeader(f);
    	} catch ( FileNotFoundException ex ) {
    		f.close();
    		hdr = null;
    		System.err.println (
    			ex.getMessage() + String.format(" at yPos %s",yPos)
    			                 );
    	} if ( hdr == null )
    		return loade;
    
    	m_type  = hdr.FORMAT;
    	if( hdr.TYPES.length == 0 ) {
    		hdr.TYPES = new TUPLTYPE[1];
    	} hdr.TYPES[0] = TUPLTYPE.valueOf( hdr.TUPELS[0] );

    	loade = parseLayerFromInputStream( f, hdr );

    	f.close();
    	return loade;
    }
        
}
