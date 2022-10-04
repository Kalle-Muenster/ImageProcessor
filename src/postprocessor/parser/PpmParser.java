package postprocessor.parser;

import java.io.FileInputStream;
import java.text.ParseException;
import org.eclipse.swt.graphics.PaletteData;

import postprocessor.color.Color32;
import postprocessor.color.Color32Surface;
import postprocessor.color.Layer32;

import org.eclipse.swt.graphics.ImageData;


//loading P3 format PNM image (.ppm) files:
public class PpmParser extends PnmParser
{
	public PpmParser( Color32Surface.FORMAT outputformat )
	{
		super( PNMTYPE.P3, outputformat );
	}

	private int proceedPPMline( int pixelsCount, String line, ImageData image ) throws ParseException
	{
    	if( !line.startsWith( "#" ) ) {
			String[] numbers = line.split( " " );
			int counter = 0;
			int numCount = numbers.length;
			for(int p=0;p<numCount;p+=3) {
			  for( int i=0; i<4; i++ ) {
				if(!(++counter>3)) {
					m_color.setChannel( m_color.layout().INDEX[counter-1], 
							            Integer.valueOf(numbers[p+i].trim()) );
				} else {
					int xPos = pixelsCount % image.width;
					int yPos = pixelsCount / image.width;
					image.setPixel( xPos, yPos, m_color.getRGB() );
					image.setAlpha( xPos, yPos, m_color.getAlpha() );
					pixelsCount++;
					counter=0;
				} 
			  }
			}
		} return pixelsCount;
    }

    public ImageData loadImage( String fileName ) throws Exception
    {
    	ImageData loade = null;
    	FileInputStream file = null;
    	try	{
    		int c=0;
    		file = new FileInputStream(fileName);
    		StringBuffer buffer = new StringBuffer();
    		java.text.NumberFormat numberParser = java.text.NumberFormat.getInstance();
    		PaletteData paletti = new PaletteData( 0xff0000, 0x00ff00,0x0000ff );
    		PNMTYPE tryConvert = m_type;
    		while( (c = file.read() ) != -1 )
    			if( c=='\n' ) break;
    			else buffer.append((char)c);
    		String line = buffer.toString();
    		if (!line.contains(m_format)) {
    			tryConvert = PNMTYPE.valueOf(line.replace("#",""));
    		} buffer.setLength(0);
    		line = "#";
    		int counter = 0;
    		while( line.startsWith("#") )
    		while( (c = file.read()) != -1 ) {
    			if( c=='#' ) {
    				buffer.append( (char)c );
    				while( (c = file.read()) != '\n' )
    					buffer.append( (char)c );
    				line = buffer.toString();
    				buffer.setLength(0);
    				break;
    			}
    			if( c=='\n' ) c=' ';
    			if( c==' ' ) {
    				if(++counter > 2) {
    					line = buffer.toString();
    					buffer.setLength(0);
    					break;
    				}
    			} buffer.append( (char)c );
    		}
    		String[] numbers = line.split(" ");
    		int[] data = new int[3];
    		counter = 0;
    		for( int i=0; i<numbers.length; i++ ) {
    			data[i] = Integer.valueOf( numbers[i].trim() );
    		} counter = 0;
    		
    		m_color.setAlpha( data[2] );
    		
    		if( tryConvert != m_type ) {
    			PamParser.PamHeader hdr = PamParser.PamHeader.fromPnmType( tryConvert, data[0], data[1] );
    		    PamParser fallbackparser = new PamParser( this.m_color.format() );
    		    fallbackparser.defaultBackgroundColor().assign( this.m_color );
    		    loade = fallbackparser.parseFromInputStream( file, hdr );
    		} else { 			   		
    			loade = new ImageData( data[0], data[1], 32, paletti );
    		    while( (c = file.read()) > -1 ) {
    		    	if( c == '\n' ) {
    		    		line = buffer.toString();
    		    		counter = proceedPPMline(
						            counter, line, loade
						                          );
					    buffer.setLength(0);
    		    	} else
    				    buffer.append( (char)c );
    		    } line = buffer.toString();
    		    if( line.length() > 3 ) {
    		        counter = proceedPPMline(
    		    	            counter, line, loade
    		    	                          );
    		    }
    		} file.close();
    	} catch ( Exception Ex ) {
    		file.close();
    		loade = null;
    		System.err.println( "Error when parsing ppm data: "+Ex.getMessage() );
    	} return loade;
    }
    
	public Layer32 loadLayer(String fileName) throws Exception
	{
		int c=0;
		FileInputStream file = new FileInputStream(fileName);
		StringBuffer buffer = new StringBuffer();
		java.text.NumberFormat numberParser = java.text.NumberFormat.getInstance();
		PaletteData paletti = new PaletteData( 0xff0000, 0x00ff00,0x0000ff );
		PNMTYPE tryConvert = m_type;
		while( (c = file.read() ) != -1 )
			if( c=='\n' ) break;
			else buffer.append((char)c);
		String line = buffer.toString();
		if (!line.contains(m_format)) {
			tryConvert = PNMTYPE.valueOf(line.replace("#",""));
		} buffer.setLength(0);
		line = "#";
		int counter = 0;
		while( line.startsWith("#") )
		while( (c = file.read()) != -1 ) {
			if( c=='#' ) {
				buffer.append( (char)c );
				while( (c = file.read()) != '\n' )
					buffer.append( (char)c );
				line = buffer.toString();
				buffer.setLength(0);
				break;
			}
			if( c=='\n' ) c=' ';
			if( c==' ' ) {
				if(++counter > 2) {
					line = buffer.toString();
					buffer.setLength(0);
					break;
				}
			} buffer.append( (char)c );
		}
		String[] numbers = line.split(" ");
		int[] data = new int[3];
		counter = 0;
		for( int i=0; i<numbers.length; i++ ) {
			data[i] = Integer.valueOf( numbers[i].trim() );
		} counter = 0;
		
		m_color.setAlpha( data[2] );
		
		PamParser.PamHeader hdr = PamParser.PamHeader.fromPnmType( tryConvert, data[0], data[1] );
	    PamParser fallbackparser = new PamParser( m_color.format() );
	    fallbackparser.defaultBackgroundColor().assign( m_color );
	    return fallbackparser.parseLayerFromInputStream( file, hdr );
	}
}
