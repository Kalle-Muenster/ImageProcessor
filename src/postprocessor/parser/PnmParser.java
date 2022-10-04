package postprocessor.parser;

import postprocessor.color.Color32;
import postprocessor.color.Color32Surface;
import postprocessor.color.Layer32;

import org.eclipse.swt.graphics.ImageData;


public abstract class PnmParser
{
	public abstract ImageData loadImage( String fileName ) throws Exception;
	public abstract Layer32   loadLayer( String fileName ) throws Exception;

	public enum PNMTYPE {
		P1,P2,P3,P4,P5,P6,P7,P8
	}

	protected final PNMTYPE[] m_supports = new PNMTYPE[] {
		PNMTYPE.P2,PNMTYPE.P5,PNMTYPE.P3,
		PNMTYPE.P6,PNMTYPE.P7,PNMTYPE.P8
	};
	protected PNMTYPE m_type;
	protected Color32 m_color;
	protected String  m_format;

	public PnmParser( PNMTYPE inputformat, Color32Surface.FORMAT outputformat )
	{
		m_type = inputformat;
		m_format = m_type.toString();
		m_color = Color32.createColor( outputformat );
		m_color.setRGBA( 0, 255 );
		if( m_format.contains(".") )
			m_format = m_format.substring( m_format.lastIndexOf('.') );
	}
	
	public Color32 defaultBackgroundColor()
	{
		return m_color;
	}
	
	private static PnmParser selectParser( String fileName, Color32Surface.FORMAT outputformat ) throws Exception
	{
		String ext = fileName.substring( fileName.lastIndexOf('.') );
		PnmParser pnmImage = null;
		switch(ext) {
		    case ".pgm": pnmImage = new PgmParser(outputformat); break;
			case ".ppm": pnmImage = new PpmParser(outputformat); break;
			case ".pnm":
			case ".pam": pnmImage = new PamParser(outputformat); break;
			default: {
				String error = String.format("unsupported PNM format '%s'",ext);
				System.err.println(error);
				throw new Exception(error);
			}
		} return pnmImage;
	}

    public static ImageData ImageFromFile( String fileName, Color32Surface.FORMAT outputformat ) throws Exception
    {
    	return selectParser( fileName, outputformat ).loadImage( fileName );
    }

	public static Layer32 LayerFromFile( String fileName, Color32Surface.FORMAT outputformat ) throws Exception 
	{
		return selectParser( fileName, outputformat ).loadLayer( fileName );
	}
}
