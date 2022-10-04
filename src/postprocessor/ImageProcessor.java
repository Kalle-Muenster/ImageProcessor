package postprocessor;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.text.ParseException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;

import javafx.geometry.Point2D;
import javafx.scene.shape.Polygon;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.scene.transform.Rotate;

import postprocessor.color.Color32Surface;
import postprocessor.color.Color32Surface.*;
import postprocessor.color.Layer32;
import postprocessor.color.Layer32.*;
import postprocessor.color.Color32;
import postprocessor.color.Image32;
import postprocessor.parser.PnmParser;
import postprocessor.parser.PamParser.PamPosition;
import postprocessor.struct.Struct;


public class ImageProcessor 
{
	public static class MarkerParameter 
	{
		public ImageProcessor.SHAPE Shape = ImageProcessor.SHAPE.UNKNOWN;
		public Rectangle            Areal = new Rectangle(0,0,64,64);
		public Color32              Color = null;
		public Layer32              Brush = null;
		public float                Lines = 2.5f;
		
		public MarkerParameter( Color32Surface.FORMAT format )
		{
			Color = Color32.createColor( format );
		}
		public MarkerParameter assign( MarkerParameter copy )
		{
			Shape = copy.Shape;
			Areal.x = copy.Areal.x;
			Areal.y = copy.Areal.y;
			Areal.width = copy.Areal.width;
			Areal.height= copy.Areal.height;
			Color.assign( copy.Color );
			Brush = copy.Brush;
			Lines = copy.Lines;
			return this;
		}
		public MarkerParameter assign( ImageProcessor.SHAPE shape, Rectangle bounds, Color32 color, float brushSize )
		{
			Shape = shape;
			Areal.x = bounds.x;
			Areal.y = bounds.y;
			Areal.width = bounds.width;
			Areal.height= bounds.height;
			if ( Lines != brushSize || Color.farb != color.farb ) {
				createBrush( color, brushSize );
			} else {
				Lines = brushSize;
				Color.assign( color );
			}
			return this;
		}
		private void createBrush( Color32 color, float dotSize )
	    { 	
	    	dotSize = dotSize < 2 ? 2.0f : dotSize;
	    	short size = (short)((dotSize / 2.0f) + 0.5f);
	    	size *= 2; size += 1; // make always odd sized!
	    	long brushsize = Struct.newPoint(size,size);
	    	int HalbSize = (short)(size/2);

	    	if ( Brush != null ) {
	    		if( Brush.Size() == brushsize )
	    			if ( Brush.getCenterPixel().farb == color.farb )
	    			return;
	    	} 
	    	Lines = dotSize;
	    	if ( Color.format() != color.format() ) {
	    		Color = color.clone();
	    	} else Color.assign( color.farb );

	    	//m_brush = new int[size*size];
	    	Brush = Layer32.createLayer( size, size, Color.format() );
	    	

	    	 // extract separate color and alpha values
	    	// from the currently selected rgba-color..   	
	    	int colorVal = Color.getRGB();
	    	int newA = Color.getAlpha();
	    	float getA = newA;

	    	int index = 0;
	    	Layer32 it = Brush.letsGo( Layer32.Forwards );
	    	do{ index = it.Index() + 1;
	    		int y = (index / size) - HalbSize;
	    		int x = (index % size) - HalbSize;
	    		float alphHalf=getA/2.0f;
	    		it.setRGBA( colorVal, (int)( ( alphHalf*( (float)(y<0?-y:y) / HalbSize ) )
  				                           + ( alphHalf*( (float)(x<0?-x:x) / HalbSize ) ) ) );
	    	} while (
	    		it.setNext( it.farb )
	    	);
	    }
		public @Struct.Point long getHalbSize() {
			return Struct.newPoint( Areal.width/2, Areal.height/2 );
		}
		public @Struct.Point long getCenter() {
			if( Shape==SHAPE.CIRCLE || Shape==SHAPE.ELLYPS || Shape==SHAPE.CROSS ) {
				return Struct.newPoint( Areal.x, Areal.y );
			} else {
				return Struct.vector( getHalbSize(), Struct.newPoint( Areal.x, Areal.y ) );
			}
		}
		public @Struct.Point long getCorner() {
			return getCorner( 0 );
		}
		public @Struct.Point long getCorner( int index ) {
			switch(index) {
			case 0: { if( Shape==SHAPE.CIRCLE || Shape==SHAPE.ELLYPS || Shape==SHAPE.CROSS )
				return Struct.vector( getHalbSize(), Struct.newPoint( Areal.x, Areal.y ) );
			  else
				return Struct.newPoint( Areal.x, Areal.y ); 
		   }case 1: { if( Shape==SHAPE.CIRCLE || Shape==SHAPE.ELLYPS || Shape==SHAPE.CROSS )
				return Struct.newPoint( Areal.x + Areal.width/2, Areal.y - Areal.height/2 );
			  else
				return Struct.newPoint( Areal.x + Areal.width, Areal.y ); 
		   }case 2: { if( Shape==SHAPE.CIRCLE || Shape==SHAPE.ELLYPS || Shape==SHAPE.CROSS )
				return Struct.newPoint( Areal.x + Areal.width/2, Areal.y+Areal.height/2 );
			  else
				return Struct.newPoint( Areal.x + Areal.width, Areal.y + Areal.height ); 
		   }case 3: { if( Shape==SHAPE.CIRCLE || Shape==SHAPE.ELLYPS || Shape==SHAPE.CROSS )
				return Struct.newPoint( Areal.x - Areal.width/2, Areal.y + Areal.height/2 );
			  else
				return Struct.newPoint( Areal.x, Areal.y + Areal.height ); 
		    } default: return Struct.newPoint(-1,-1);
		  }
		}
		public Rectangle fitWithin( @Struct.Point long imgSize ) {
			Rectangle clipped = new Rectangle( Areal.x, Areal.y, Areal.width, Areal.height );
			long corner = getCorner();
			int min = Struct.getX( corner );
			int max = min + Areal.width;
			if (min < 0) {
				 clipped.x -= min;
				 clipped.width += min;
			}
			if ( max >= Struct.getX( imgSize ) ) {
				clipped.width -= (max - Struct.getX(imgSize));
			}
			min = Struct.getY( corner );
			max = min + Areal.height;
			if ( min < 0 ) {
				 clipped.y -= min;
				 clipped.height += min;
			}
			if (max >= Struct.getY(imgSize)) {
				clipped.height -= (max - Struct.getY(imgSize));
			}
			return clipped;
		}
	}
	
	public static void displayUsage() {
		System.out.print("\n\n" + " ImageProcessor usage: \n" + "\n"
				+ "    ImageProcessor <PathToImageFile> [Command1 [ParameterList1]] [Command2 [ParameterList2]] [Command...]]...\n"
				+ "\n" + "    aviable commands:\n\n\n"
				+ "        cuttenEdges   : Removes [bordersize] pixels from the image's edges (for removing background\n"
				+ "                        which may be visible at the edges, behind windows.) when used without parameters,\n"
				+ "                        it will use 3 1 3 3 for \"left top right bottom\" by derfault.\n"
				+ "                        usage:\n"
				+ "                                cuttenEdges [borderSize|<left top right bottom>]\n\n"
				+ "        cropFrame     : Crops the area [X Y Width Height] out of the image by discarding\n"
				+ "                        anything else which would be outside this defined rectangle.\n"
				+ "                        usage:\n" + "                                cropFrame <X Y W H>\n\n"
				+ "        roundCorners  : Roundens a taken image's corners (by default 4,5 pixel) or by given parameter.\n"
				+ "                        When given a second parameter, first parameter will be used for cutting\n"
				+ "                        the upper corners, and the second parameter for cutting the lower corners\n"
				+ "                        usage:\n"
				+ "                                roundCorners [CornerSize|UpperCorners LowerCorners]\n\n"
				+ "        rotateImage   : Rotates the whole image for N steps (-90° per step). also\n"
				+ "                        giving absolute degree values of 90°, 180° or 270° is supported:\n"
				+ "                        usage:\n"
				+ "                                rotateImage [+/-]<N> | [+/-]<90|180|270>\n\n"
				+ "        flipImage     : Flips the whole image eitrher 'UP' direction or 'RIGHT' direction:\n"
				+ "                        usage:\n"
				+ "                                flipImage <UP|RIGHT>\n\n"
				+ "        inlineImage   : Draws an inline at <bordersize> pixel strange round the inner edges of the image at actual set color\n"
				+ "                        usage:\n"
				+ "                                inlineImage <bordersize>\n\n"
				+ "        outlineImage  : Draws an outline at <bordersize> thickness round the image's outer edges, at actual set color\n"
				+ "                        usage:\n"
				+ "                                outlineImage <bordersize>\n\n"
				+ "        setColor      : Set the color to be used for drawing markers:\n"
				+ "                        takes 1x int-value (hex or dec) or 3 to 4 separated byte-values:\n"
				+ "                        usage:\n"
				+ "                                setColor <rgb|rgba|R G B [A]>\n\n"
				+ "                        Also possible are 'modifiers' for changing the actual loaded color state\n"
				+ "                        like:\n"
				+ "                                setColor <Dark|Light|Soft|Opaque|Invert|Accent>\n\n"
				+ "                        Also supported are strings representing Named colors as like 'Red', 'Blue'\n"
				+ "                        'Orange', or 'Pink' (Case-Insensitive). As well combinations of these are\n"
				+ "                        possible and may be given dot '.' separated, like such this\n"
				+ "                        color:\n"
				+ "                                setColor dark.Green.gray  or setColor orange.deep.Red\n\n"
				+ "        drawLine      : Draws a line from point <X1 Y1> to point <X2 Y2> at optional strength [S]:\n"
				+ "                        usage:\n" 
				+ "                                drawLine <X1 Y1 X2 Y2> [S]\n\n"
				+ "        drawMarker    : Draws a marker overlay to the loaded Image\n"
				+ "                        As first parameter can be given a 'SHAPE' argument which decides how any\n"
				+ "                        following number parameters will be interpreted for drawing the marker:\n"
				+ "                         - ARROW (followed by 4 parameters) will draw an arrow pointing from P1/P2 to P3/P4.\n"
				+ "                         - CROSS (followed by 3 parameters) will draw a crosshair at P1/P2 by radius P3.\n"
				+ "                         - CIRCLE (followed by 4 parameters) draws an ellypsoid, with center at P1/P2 at size P3/P4.\n"
				+ "                         - SQUARE (followed by 3 parameters) draws a square rectangle (top-left: P1/P2, size: P3)\n"
				+ "                        If the SHAPE parameter is ommitted, the command then will 'guess' a SHAPE\n"
				+ "                        on it's own by the count on positioning parameters which may follow:\n"
				+ "                          - 2 parameters are interpreted as: Arrow (pointing good visible to point P1/P2).\n"
				+ "                          - 3 parameters are interpreted as: Circle (centered at P1/P2 with radius P3).\n"
				+ "                          - 4 parameters are interpreted as: Rectangle (top-left corner P1/P2 with size P3/P4)\n"
				+ "                        usage:\n"
				+ "                                drawMarker [SHAPE] <posX> <posY> [width [height]]\n\n"
				+ "        showObject    : Shows up an object within an image by pointing it with an ARROW marker\n"
				+ "                        at clearly visible size and position. If given 2 parameters, and point\n"
				+ "                        lays inside last drawn CIRCLE or SQUARE marker area, it will point it's\n"
				+ "                        area's center position then. If point does not lay within last\n"
				+ "                        drawn area, it will just point out the given coordinates directly.\n"
				+ "                        If given 4 parameters, it will draw a marker [X Y W H] plus\n"
				+ "                        an ARROW pointing that last SHAPE markers center position then by\n"
				+ "                        using complement of the color which is currently sellected drawing color.\n" 
				+ "                        usage:\n"
				+ "                                showObject [SHAPE] <posX> <posY> [Width [Height]]\n\n"
				+ "        dragObject    : Visulizes a planed 'drag' operation and (if maybe different) the actual\n"
				+ "                        result of a planed drag operation when it was completed at least.\n"
				+ "                        Takes a SHAPE parameter and positioning parameter P1/P2 to P3/P4. If P1/P2 lays\n"
				+ "                        inside a prior (directly before) drawn CIRCLE or SQUARE marker, it will use\n"
				+ "                        that marker's center position then as the 'dragling' instead\n"
				+ "                        usage:\n"
				+ "                                dragObject [SHAPE] <fromX> <fromY> <toX> <toY>\n\n"
				+ "    Within a single ImageProcessor call, as many commands it's needs for completing a desired image\n"
				+ "    can be chained after each other.\n\n    Following example:\n\n"
				+ "        ImageProcessor someImageFile.png cuttenEdges roundCorners 2,5 0 drawMarker 100 75 50 setColor 255 128 0 drawMarker 300 250 400 270\n\n"
				+ "    would load an image, then cuts 3 pixels from each of it's edges, rounds it's upper-left and upper-right corners by radius of 2.5pixel and then\n"
				+ "    it draws a red circle of 50px radius at position x:100 y:75 and an orange 400px X 270px pixel rectangle at position x:300 y:250."
				+ "\n");
	}
	    
	    private static final double ARROW_FACTOR = 0.5/3.0; 
	    public enum SHAPE { CIRCLE, ELLYPS, SQUARE, ARROW, CROSS, UNKNOWN }
        public enum DIRECTION { UP, RIGHT, DOWN, LEFT }
        
		private ImageData         m_image;
	    private int               m_edgeTop = 1;
	    private int               m_edgeLeft = 3;
	    private int               m_edgeRight = 3;
		private int               m_edgeBottom = 3;
	    private float             m_cornerSize;
	    private float[]           m_cornerKreis;
	    private String            m_fileName;
	    private String            m_originalName;
	    private boolean           m_changeName = true;
	    private boolean           m_isMainCall = true;
        private final ImageLoader m_loader;       
	    private int[]             m_brush = new int[16];
		private Color32           m_color = new Color32.BGRA(0x2010FF,200);
		private Color32           m_lineColor = new Color32.BGRA(0x2010FF,200);
		private float       	  m_circleBorder = 2.5f;
	    private short             m_dotHalbSize;
	    private MarkerParameter   m_lastMarker = new MarkerParameter(FORMAT.BGRA);
	    public MarkerParameter    CurrentMarker = new MarkerParameter(FORMAT.BGRA);
	    private Image32           m_view;

	    // constructor to use when imported by other source.
	    ImageProcessor( Image image, String fileName )
	    {
	        m_loader = new ImageLoader();
	        m_loader.data = new ImageData[1];
	        m_image = image.getImageData();
	        findMatchingFormatCode( m_image );
	        m_loader.data[0] = m_image;
	        m_fileName = fileName;
	        m_originalName = m_fileName;
	        calcCirquad(4.5f);
	        calcDotquad(m_circleBorder);
	    }
	    
	    // constructor used when called from command line.
	    ImageProcessor(String filePath) throws Exception
	    {
	        m_loader = new ImageLoader();
	        m_loader.data = new ImageData[1];
	        Layer32 loade = null;
	        
	        String extension = filePath.substring( filePath.lastIndexOf('.') );
	        switch( extension ) {
	        case ".pnm":
	        case ".ppm":
	        case ".pam":
	        	loade = PnmParser.LayerFromFile( filePath, m_color.format() );
	        	m_image = null;
	            break;
	        case ".png":
	        	m_image = new ImageData(filePath);
	        	break;
	        case ".jpeg":
	        case ".jpg":
	        	m_image = new ImageData(filePath);
	        	break;
	        default: try {
	        	m_image = m_loader.load(filePath)[0];
	        	} catch (Exception ex) {
	        		System.err.println( 
	        			String.format("Error: [%s] during loading image from file: %s\n",ex.getMessage(),filePath)
	        		);
	        		throw ex;
	        	} 
	        }
	        m_fileName = filePath.replace( extension, ".png" );
	    	m_originalName = m_fileName;
	    	

	        Color32Surface.FORMAT formatcode = Color32Surface.FORMAT.BGRA; //  new Color32Surface.SURFACECODE(Color32Surface.FORMAT.BGRA); //.fromChannelOrder("rgba").format;
	        
	        if ( loade == null ) {
	        	
	        	checkAlphaChannelData( m_image );
	        	loade = new Layer32(
	        		Struct.newPoint( m_image.width, m_image.height ),
	        		formatcode = findMatchingFormatCode( m_image )
	        	);        	

	        	Layer32 iterator = loade.letsGo( Layer32.Forwards );
	        	while( iterator.nextStep() ) {
	        		iterator.setPixel(
	        			m_image.getPixel( iterator.PointX(), iterator.PointY() ),
	        			m_image.alphaData[iterator.Index()]
	        		);
	        	}

	        } else if ( m_image == null  ) {

	        	formatcode = loade.format(); 
	        	Color32Surface.Layout model_C32 = 
	        			new Color32Surface.Layout( formatcode );
	        	
	        	PaletteData model_SWT = new PaletteData(
	        		model_C32.MASKS[model_C32.INDEX[0]],
	        		model_C32.MASKS[model_C32.INDEX[1]],
	        		model_C32.MASKS[model_C32.INDEX[2]]
	        	);
	        	m_image = new ImageData( loade.Width(),
	        			 loade.Height(), 24, model_SWT
	        			                  );
	     
	        	if ( m_image.alphaData == null ) {
	        		System.out.println( "image data was allocated with no alpha channel buffer" );
	        		m_image.alphaData = new byte[
	        		    Struct.indices( loade.Size() )
	        		];
	        	}
	        	
	        	Layer32 pixel = loade.letsGo( Layer32.Forwards );
	        	while( pixel.nextStep() ) {
	        		m_image.setPixel( pixel.PointX(), pixel.PointY(), pixel.getRGB() );
	        		m_image.alphaData[pixel.Index()] = (byte)pixel.getAlpha();
	        	}
	        }

	        m_loader.data[0] = m_image;
	        Color32 correct = Color32.createColor( loade.format() );
	        correct.assignChecked( m_color );
	        m_color = correct;
	        
	        System.out.print( String.format(
	        		"Image: loaded (size %s x %s, %s): %s\n",
	        		 m_image.width, m_image.height, formatcode.toString(), filePath )
	        		           );

			m_view = new Image32( loade );
	        calcCirquad( 4.5f );
            createBrush( m_circleBorder, 1.0f );
	    }

	    
	    // private helper functions:
	    
	    private boolean nextIsSubCall(boolean isASubCall) 
	    {
	    	boolean laststate = !m_isMainCall;
	    	m_isMainCall = !isASubCall;
	    	return laststate;
	    }
	       
	    private Color32Surface.FORMAT findMatchingFormatCode( ImageData img )
	    {
	    	String colorchannels;
            if( img.palette.redShift < img.palette.greenShift ) {
            	colorchannels = "BGRA"; 
            } else {
            	colorchannels = "RGBA";
            }

            Color32Surface.FORMAT fcode;
            try { fcode = Color32Surface.SURFACECODE.fromChannelOrder(colorchannels).format; 
            } catch( Color32.UnknownFormat ex ) {
            	fcode = Color32Surface.FORMAT.INVALID;
            }
	        return fcode;
	    }
	    
	    private boolean checkAlphaChannelData( ImageData img )
	    {
	    	boolean hasAlphaChannel = false;
            int depth = img.depth; 
	        if( img.depth != 32 ) { // check if loaded image is NOT 32bit 
	        	if ( img.alphaData != null ) { // check if a valid alpha channel was loaded anyway maybe 
	        		if( img.alphaData.length == img.data.length/3 )
	        			img.depth = 32; // and correct depth flag for indicating 32bit then when
	        		                   // there really a fourth color channel was loaded from file
	        	} if ( img.depth != 32 ) { 
	        		 // if image really has no alpha channel present, 
	        		// then add an opaque alpha channel buffer:
	        		img.alphaData = new byte[img.width*img.height];
	        		for(int i=0;i<img.width*img.height;i++) {
	        			img.alphaData[i]=(byte)255;
	        		} hasAlphaChannel = true;
	        	}
	        } else hasAlphaChannel = true;
	        // and set the image's bitdepth flag back to the value set by the loader
	        img.depth = depth;
	        return hasAlphaChannel;
	    }
	    
	    private void calcCirquad(float size)
	    {
	         // calculates quarter of a circle and store 
	    	// it, so later can be used by roundCorners
	        m_cornerSize = size;
	        size = (size*2-1);
	        m_cornerKreis = new float[(int)size];
	        for(int i=0;i<size;i++)
	            m_cornerKreis[i] = size - (float)Math.sqrt(Math.pow(size,2)-Math.pow(size-(i+1),2)); 
	    }

	    private void cutOneCorner( int oA, int oB )
	    {
	        // get directions:
	        int dA = oA==0?1:-1;
	        int dB = oB==0?1:-1;
	        
	        // correct outermost's to be width/height - 1 
	        oA = oA+(dA-1)/2; 
	        oB = oB+(dB-1)/2;

	        int merk = m_color.farb;
	        for(int b=0; b<m_cornerKreis.length; ++b )
	        {// set alpha values of all pixels laying outside the 'cornerKreis' to 0
	        	int p = (int)m_cornerKreis[b];
	            int cB = oA+((oB+(b*dB))*m_view.W);
	            for( int a=0; a<p; ++a ) {
	                m_color.farb = m_view.layer.setPosition((a*dA)+cB).getValue();
	                m_color.setAlpha(0);
	                m_view.layer.setPixel( m_color.farb );
	            } // for inner most pixels, use these decimal digits which where gone lost during
	            // float-to-int conversion, to soften each processed pixel by that percentage.
	            cB = ((p*dA)+cB);
	            m_color.farb = m_view.layer.setPosition(cB).getValue();
	            m_color.setAlpha( (short)(m_color.getAlpha() * (1.0f-(m_cornerKreis[b]-p))) );
	            m_view.layer.setPixel( m_color.farb );
	        }
	        // and then also soften the inner most pixels going along the y-direction
	        for(int a=0;a<m_cornerKreis.length;++a) {
	            int p = (int)m_cornerKreis[a];
	            int cA = (oA+(a*dA))+((oB+p*dB)*m_view.W);
	            m_color.farb = m_view.layer.setPosition(cA).getValue();
	            m_color.setAlpha( (short)(m_color.getAlpha() * (1f-(m_cornerKreis[a]-p))) );
	            m_view.layer.setPixel( m_color.farb );
	        }
	    }

	    private void calcDotquad( float dotSize ) { createBrush(dotSize, 1.0f); }
	    private void createBrush( float dotSize, float presure )
	    {
	         // calculate a brush at size dotSize,
	    	// which will be used for drawing lines
	    	
	    	dotSize = dotSize < 2 ? 2.0f : dotSize;
	    	short size = (short)((dotSize / 2.0f) + 0.5f);
	    	size *= 2; size += 1; // make always odd sized!
	    	long brushsize = Struct.newPoint(size,size);
	    	
	    	if ( CurrentMarker.Brush != null ) {
	    		if( CurrentMarker.Brush.Size() == brushsize )
	    			if ( CurrentMarker.Color.farb == m_color.farb )
	    			return;
	    	} 
	    	CurrentMarker.Lines = dotSize;
	    	
	    	//m_brush = new int[size*size];
	    	CurrentMarker.Brush = m_view.layer.newLayer( brushsize, 0 );
	    	m_dotHalbSize = (short)(size/2);

	    	 // extract separate color and alpha values
	    	// from the currently selected rgba-color..   	
	    	int color = m_color.getRGB();
	    	int newA = (int)(m_color.getAlpha()*presure);
	    	float getA = newA;
	    	
	    	// extract the currently used ColorFormat
	    	Color32Surface.FORMAT format = m_color.format();
	    	int index = 0;
	    	Layer32 it = CurrentMarker.Brush.letsGo( Layer32.Forwards );
	    	do{ index = it.Index() + 1;
	    		int y = (index / size) - m_dotHalbSize;
	    		int x = (index % size) - m_dotHalbSize;
	    		float alphHalf = getA/2.0f;
	    		it.setRGBA( color, (int)( ( alphHalf*( (float)(y<0?-y:y) / m_dotHalbSize ) )
  				                        + ( alphHalf*( (float)(x<0?-x:x) / m_dotHalbSize ) ) ) );
	    	} while (
	    		it.setNext( it.farb )
	    	);
	    }

	    private void advancedDrawPixel( Color32 upperLayer /*or brush tip*/, Color32 lowerLayer /* draw on layer */, Color32 targetLayer /* opaque BG or distant projection  */ )
	    {
	    	if( targetLayer == null ) {
	    		targetLayer = lowerLayer;
	    	}
	    	if( lowerLayer.getAlpha() == 0 ) {
	    		targetLayer.assign( upperLayer );
	    	} else {
	    		targetLayer.assign( upperLayer.layOverRGBA( lowerLayer ) );
	    	}
	    }
	    	
	    private void drawPixel( int X, int Y, Color32 brush )
	    {	
            Color32 pix = m_view.layer.setPosition(X,Y).getPixel();
            if( pix.getAlpha() == 0 ) {
            	m_view.layer.setPixel( brush.farb );
            } else {
            	m_view.layer.setPixel( brush.layOverRGBA( pix ) );
           }
	    }
	    
	    private void drawPixelChecked( int X, int Y, Color32 color )
	    {	
	    	if( X >= 0 && X < m_view.layer.Width() && Y >= 0 && Y < m_view.layer.Height() )
	    		drawPixel( X, Y, color );
	    }
	    
	    // draws like drawPixel() but by using a brush (of size m_dotHalbSize*2 )
	    private void drawDot( int X, int Y, boolean checked )
	    {
	    	CurrentMarker.Brush.moveLayer( X - m_dotHalbSize, Y - m_dotHalbSize );
	    	Layer32 dot = CurrentMarker.Brush.letsGo( Layer32.Forwards );
	    	for( int y = -m_dotHalbSize; y <= m_dotHalbSize; ++y ) {
	    		for( int x = -m_dotHalbSize; x <= m_dotHalbSize; ++x ) {
	    			if( checked )
	    				drawPixelChecked( X+x, Y+y, dot.getNext() );
	    			else 
	    				drawPixel( X+x, Y+y, dot.getNext() );
	    		}
	    	}
	    }
	
	    private void drawCircle()
	    {
	    	int W = m_view.W;
	    	int H = m_view.H;
	    	long c = CurrentMarker.getCenter();
	    	long s = CurrentMarker.getHalbSize();
	    	int v1 = Struct.getX(c);
	    	int v2 = Struct.getX(s);
	    	boolean clip_image = !(v1-v2 >= 0 && v1+v2 < W);
	    	if( !clip_image ) {
	    		v1 = Struct.getY(c); v2 = Struct.getY(s);
	    		clip_image = !(v1-v2 >= 0 && v1+v2 < H);
	    	} if ( clip_image ) {
	    		System.err.println("Image: WARNING - marker '"+CurrentMarker.Shape.toString()+"' shape may exceed image bounds!");
	    	}

	    	// reset the brush if it's not at Circle-Border-Default size anymore:
	    	float strength = Math.min(CurrentMarker.Areal.width,CurrentMarker.Areal.height)/10;
	    	strength = ( strength < m_circleBorder ) ? strength : m_circleBorder;
	    	createBrush( strength, 1.0f );
	    	
	    	// place the circl's bounding rectangle by it's center-position!
	        float centerX = CurrentMarker.Areal.width/2;
	        float centerY = CurrentMarker.Areal.height/2;
	        
	        // the Y-scale factor to form the circle ellipsoid     
	        float scale = (float)CurrentMarker.Areal.height/CurrentMarker.Areal.width;
	        float Squared = (float)Math.pow(centerX,2);
	            
	        short storedAlpha = m_color.getAlpha();
	        m_color.setAlpha((storedAlpha*32)/255);
	        int minX,minY,maxX,maxY;
	        minY = minX = m_dotHalbSize;
	        maxX = W - m_dotHalbSize;
	        maxY = H - m_dotHalbSize;
	        
	        int yLast = (int)centerY; 
	        for (int x = (int) -centerX; x < centerX + 1; x++)
	        {
	            int y  = (int)( 0.5f + centerY - Math.sqrt( Squared - Math.pow(x, 2) ) * scale );
	            int Y2 = (int)( ( (CurrentMarker.Areal.y + CurrentMarker.Areal.height) - y) - centerY );
	            int Y1 = (int)( (CurrentMarker.Areal.y + y) - centerY );
	            int X  = (int)( CurrentMarker.Areal.x + x );
                drawDot( X, Y1, clip_image );  // ...for upper half the circle
	            drawDot( X, Y2, clip_image ); // ...and lower half the circle
	                    
	            // connect these dots by lines, to form the circle!
	            int delta = y - yLast;
	            int direction = delta > 1 ? 1 : delta < -1 ? -1 : 0;
	            if (direction != 0) {
	                for (int i = 0; i != delta; i += direction) {
	                    Y2 = (int)( ( (CurrentMarker.Areal.y + CurrentMarker.Areal.height) - (yLast + i) ) - centerY );
	                    Y1 = (int)( ( CurrentMarker.Areal.y + (yLast + i) ) - centerY );
	                    X = (int)( CurrentMarker.Areal.x + (x - (direction > 0 ? (i < (delta / 2) ? 1 : 0) : i > (delta / 2) ? 1 : 0 ) ) );
	                    drawDot( X, Y1, clip_image );
	                    drawDot( X, Y2, clip_image );
	                }
	            }

	            // and make the inner circle being transparent.
	            if ( clip_image ) {
	            	for( int trY=Y1; trY<Y2; ++trY )
	            		drawPixelChecked( X, trY, m_color );
	            } else {
	            	for( int trY=Y1; trY<Y2; ++trY )
	            		drawPixel( X, trY, m_color );
	            } yLast = y;
	        }
	        m_color.setAlpha( storedAlpha );
	    }
	    
	    
		public void drawLine(int fromX,int fromY,int toX,int toY)
		{
			drawLine( Struct.newPoint(fromX, fromY), Struct.newPoint(toX,toY), CurrentMarker.Lines, m_color, 1 );
		}
		public void drawLine(@Struct.Point long from,@Struct.Point long to)
		{
			drawLine( from, to, CurrentMarker.Lines, m_color, 1 );
		}
		public void drawLine(@Struct.Point long from,@Struct.Point long to, Color32 color)
		{
			drawLine( from, to, CurrentMarker.Lines, color, 1 );
		}
		public void drawLine(@Struct.Point long from,@Struct.Point long to, float strength)
		{
			drawLine( from, to, strength, m_color, 1 );
		}
		public void drawLine(@Struct.Point long a, @Struct.Point long b, float strength, Color32 color)
		{
			drawLine( a, b, strength, color, 1 );
		}
		public void drawLine(@Struct.Point long a,@Struct.Point long b, float strength, Color32 color, float shorten)
		{
			if((m_lineColor.getValue() != color.getValue()) || (strength != CurrentMarker.Lines)) {
				if((strength != CurrentMarker.Lines))
					System.out.printf("Image: set brush size to: [%f3px]\n",strength);
				m_lineColor.assign( m_color );
				m_color.assign( color );
				createBrush( strength, 1.0f );
				m_color.assign( m_lineColor );
				m_lineColor.assign( color );
			}
			
			int aX = Struct.getX(a);
			int aY = Struct.getY(a);
			int bX = Struct.getX(b);
			int bY = Struct.getY(b);
			int tX = 0;
			int tY = 0;

			if (aX>bX) {
				tX=aX;
				tY=aY;
				aX=bX;
				aY=bY;
				bX=tX;
				bY=tY;
			} 
			
			int dX = bX-aX;
			int dY = bY-aY;
			int flyp = dY<0?-dY:dY;
			
			if(dX>=flyp) { 
				float sY = (float)dY/dX;
				float Y = aY;
				int brak = (int)(dX*shorten);
				for(tX=aX;tX!=bX;tX+=1) {
					tY = (int)( sY>0 ? Y+0.5 : Y-0.5 );
					drawDot((short)tX,(short)tY,true);
					Y+=sY;
					brak--;
					if(!(brak>0))
						break;
				}
			} else {
				float sX = (float)dX/flyp;
				float X = aX;
				int sY = aY>bY?-1:1;
				int brak = (int)(flyp*shorten);
				for(tY=aY;tY!=bY;tY+=sY) {
					tX = (int)( sX>0 ? X+0.5 : X-0.5 );
					drawDot((short)tX,(short)tY,true);
					X+=sX;
					brak--;
					if(!(brak>0))
						break;
				}
			}
			
			if(m_isMainCall) {
				// Output some info if called from command-line
		        System.out.printf("Image: drawLine from [%s:%s] to [%s:%s]\n",aX,aY,bX,bY);
		        // change the filename under which the image will be saved later.
	            m_fileName = m_fileName.replace(".png","_line.png");
			}
					
		}

		 // helper function which takes an array of points
		// and draws lines connecting these to form a polygon
		private void connectPoints(@Struct.Point long[] points)
		{
			boolean callstate = nextIsSubCall(true);
			for(int i=1;i<points.length;i++) {
				drawLine(points[i-1],points[i]);
			} nextIsSubCall(callstate);
		}
		
		private void drawCross(int posX,int posY,int size)
		{
			int gape = (size>=100)
					 ? (int)(size/1.33)
					 : (size>=25)
					        ? (int)(size/5)
					        : (size<=15)
					                ? 3
					                : (size/3);
			if( gape==0 || size<=10 ) {
				drawLine(Struct.newPoint(posX-size,posY),
						 Struct.newPoint(posX+size,posY));
				drawLine(Struct.newPoint(posX,posY-size),
						 Struct.newPoint(posX,posY+size));
			} else {
			drawLine(Struct.newPoint(posX-size,posY),
					 Struct.newPoint(posX-gape,posY));
			drawLine(Struct.newPoint(posX+gape,posY),
					 Struct.newPoint(posX+size,posY));
			drawLine(Struct.newPoint(posX,posY-size),
					 Struct.newPoint(posX,posY-gape));
			drawLine(Struct.newPoint(posX,posY+gape),
					 Struct.newPoint(posX,posY+size));
			}
		}
		
		public void drawCrossHair(int posX,int posY,int size) 
		{
			CurrentMarker.Areal.x = posX;
			CurrentMarker.Areal.y = posY;
			CurrentMarker.Areal.width = 
			CurrentMarker.Areal.height = ( 2 * size );
			boolean callstate = nextIsSubCall( true );
			if ( size>=30 ) {
				drawCircle();
			}   drawCross( posX, posY, size );
			if ( size>=100 ) {
				drawCross( posX, posY, size/3 );
			} nextIsSubCall( callstate );
		}
		
		private void drawArrow()
		{
			if( CurrentMarker.Areal.width<0 )
				drawArrow( CurrentMarker.Areal.x,CurrentMarker.Areal.y );
			else
				drawArrow( CurrentMarker.Areal.x,CurrentMarker.Areal.y,
						   CurrentMarker.Areal.width,CurrentMarker.Areal.height );
		}
		
		public void drawArrow(int toX,int toY)
		{
			Point from = new Point( m_view.W/2, m_view.H/2 );
			Rectangle frame = new Rectangle( from.x-from.x/2, from.y-from.y/2, from.x, from.y );
			if( frame.contains( new Point(toX,toY) ) ) {
				frame.y = m_view.H / 100;
			    frame.x = m_view.W / 100;
				frame.width = m_view.W-(2*frame.x);
				frame.height = m_view.H-(2*frame.y);
			} 
			from.x = frame.x + ( toX>from.x ? 0 : frame.width );
			from.y = frame.y + ( toY>from.y ? 0 : frame.height );
			drawArrow( from.x, from.y, toX, toY );
		}
		
		public void drawArrow(int fromX, int fromY, int toX, int toY)
		{
			if( fromX < toX ) {
				m_edgeLeft = fromX;
				m_edgeRight = toX;
			} else {
				m_edgeLeft = toX;
				m_edgeRight = fromX;
			}
			if( fromY < toY ) {
				m_edgeTop = fromY;
				m_edgeBottom = toY;
			} else {
				m_edgeTop = toY;
				m_edgeBottom = fromY;
			}

			//TODO: turning this into some final static 'form' primitive better
			Point2D[] points = new Point2D[] { 
					new Point2D( -0.5, 0 ), 
					new Point2D( ARROW_FACTOR, ARROW_FACTOR ), 
					new Point2D( ARROW_FACTOR,-ARROW_FACTOR ),
					new Point2D( 0, 0.5), new Point2D( 0, -0.5 ),
					new Point2D( 0.5, 0 ) 
			};

			boolean flYps = (toY-fromY) < 0;
						
			Point2D delta = new Point2D( toX, toY ).subtract( new Point2D( fromX, fromY ) );
			Translate tr = new Translate( (delta.getX()/2.0) + fromX, (delta.getY()/2.0) + fromY );
			
			double val = delta.angle( Point2D.ZERO.add(1,0) );
			val = flYps ? -val : val; 
			Rotate ro = new Rotate( val, 0, 0 );	
			
			val = delta.magnitude();
			Scale sc = new Scale( val, val/2.0 );
			
			for( int i = 0; i < 6; ++i ) {
				Point2D p = sc.transform( points[i] ); 
				p = ro.transform(p);
				points[i] = tr.transform(p); 
				//points[i] = tr.transform( ro.transform( sc.transform( points[i] ) ) ); 
				int max = (int)(points[i].getX()+0.5);
				if( max > m_edgeRight )
					m_edgeRight = max;
				else if( max < m_edgeLeft )
					m_edgeLeft = max;
				max = (int)(points[i].getY()+0.5);
				if(max>m_edgeBottom)
					m_edgeBottom = max;
				if(max<m_edgeTop)
					m_edgeTop = max;  
			} 
			
			Polygon pol = new Polygon( 
				points[0].getX(),
				points[0].getY(),
				points[1].getX(),
				points[1].getY(),
				points[3].getX(),
				points[3].getY(),
				points[5].getX(),
				points[5].getY(),
				points[4].getX(),
				points[4].getY(),
				points[2].getX(),
				points[2].getY()
			);
			
			int stored = m_color.getAlpha();
			m_color.setAlpha( (stored*32)/255 );
			boolean overEdged = false;
			for( int x=m_edgeLeft; x<m_edgeRight; ++x ) {
				for( int y=m_edgeTop; y<m_edgeBottom; ++y ) {
					if( x<m_view.W && y<m_view.H && x>=0 && y>=0) {
						if( pol.contains( Point2D.ZERO.add( x, y ) ) )
							drawPixel( x, y, m_color );
					} else {
						overEdged = true;
					}
				}
			} m_color.setAlpha( stored );
			
			long[] pts = new long[4];
			
			pts[0] = Struct.newPoint( (int)(points[0].getX()+0.5), (int)(points[0].getY()+0.5) );
			pts[3] = Struct.newPoint( (int)(points[5].getX()+0.5), (int)(points[5].getY()+0.5) );
			
			pts[1] = Struct.newPoint( (int)(points[1].getX()+0.5), (int)(points[1].getY()+0.5) );
			pts[2] = Struct.newPoint( (int)(points[3].getX()+0.5), (int)(points[3].getY()+0.5) );
			
			connectPoints( pts ); // draw 0->1->2->3 (0 is 'from' point, 3 is 'to' point)
			
			pts[1] = Struct.newPoint( (int)(points[2].getX()+0.5), (int)(points[2].getY()+0.5) );
			pts[2] = Struct.newPoint( (int)(points[4].getX()+0.5), (int)(points[4].getY()+0.5) );
			
			connectPoints( pts ); // again draw 'from' point 0 'to' point 3, with way points (1 and 2) exchanged 
			
	        // Output some info if called from command-line
			if(m_isMainCall) {
				if(overEdged) 
					System.err.println("Image: WARNING - marker 'ARROW' shape may exceed image bounds!");
				System.out.print( "Image: Add ARROW pointing from: ["
	                            + Struct.getX(pts[0])+","+Struct.getY(pts[0])+ "] to ["
	        		            + Struct.getX(pts[3])+","+ Struct.getY(pts[3])+"]\n" );
			}
		}

		private void drawSquare()
		{
			drawSquare( true );
		}
	    private void drawSquare( boolean filled )
	    {	    	
	    	boolean fit_image = CurrentMarker.Areal.x >= 0 && CurrentMarker.Areal.x + CurrentMarker.Areal.width < m_view.W;
	    	if( fit_image )
	    		fit_image = CurrentMarker.Areal.y >= 0 && CurrentMarker.Areal.y + CurrentMarker.Areal.height < m_view.H;
	    	if(!fit_image ) {
	    		System.err.println("Image: WARING - Rectangle marker exceeds image bounds! Rectangle will be clipped!");
	    		CurrentMarker.Areal = CurrentMarker.fitWithin( Struct.newPoint( m_view.W, m_view.H ) );
	    	}
	    	
	    	int alph = m_color.getAlpha();
	    	int tr1 = (alph*192)/255;
	    	int tr2 = (alph*127)/255;
	    	int tr3 = filled ? (alph*32)/255 : 0;
	    	
	    	for( int x=0; x<CurrentMarker.Areal.width; ++x )
	    		for( int y=0; y<CurrentMarker.Areal.height; ++y )
	    		{
	    			if(( x==0 || y==0
	    			  || x==CurrentMarker.Areal.width-1
	    			  || y==CurrentMarker.Areal.height-1 ))
	    				m_color.setAlpha( tr1 );
	    			else
	    			if(( x==1 || y==1
	    			  || x==CurrentMarker.Areal.width-2
	    			  || y==CurrentMarker.Areal.height-2 ))
	    				m_color.setAlpha( alph );
	    			else
	    			if(( x==2 || y==2
	    			  || x==CurrentMarker.Areal.width-3
	    			  || y==CurrentMarker.Areal.height-3 ))
	    				m_color.setAlpha( tr2 );
	    			else
	    				m_color.setAlpha( tr3 );
	    			
	    			int X = x + CurrentMarker.Areal.x;
	    			int Y = y + CurrentMarker.Areal.y;
	    			drawPixel( X, Y, m_color );
	    		}
	    	m_color.setAlpha( alph );
	    }
	    
	    public void outlineImage( float strokeSize )
	    {
	    	int expand = (int)( strokeSize+0.5 );
	    	Layer32 newLayer = new Layer32(
	    		Struct.offset( m_view.layer.Size(), Struct.newPoint( expand*2, expand*2 ) ),
	    			           m_view.layer.format() );
	    	
	    	int newW = newLayer.Width();
	    	int newH = newLayer.Height();
            for( int y = 0; y<m_view.H; ++y ) 
            for( int x = 0; x<m_view.W; ++x ) {
              	int eXpand = x + expand;
              	int Ypspand= y + expand;
                newLayer.setPosition( eXpand, Ypspand ).setPixel( 
                	m_view.layer.setPosition( x, y ).getValue()
                );
            }
            for( int y=0; y<expand; ++y )
            for( int x=0; x<newW; ++x ) {
            	newLayer.setPosition( x, y ).setPixel( m_color.farb );
            	newLayer.directYps( newH - (y+y+1)
            			           ).setPixel( m_color.farb );
            }
            for( int y=0; y<newH; ++y )
            for( int x=0; x<expand; ++x ) {
            	newLayer.setPosition( x, y ).setPixel( m_color.farb );
            	newLayer.directX( newW - (x+x+1)
            			         ).setPixel( m_color.farb );
            }
            
            // für später...
            // newLayer.addLayer( m_view.layer, Struct.newPoint(expand,expand) );
            
            m_view.W = newLayer.Width();
            m_view.H = newLayer.Height();
            newLayer.addLayer( CurrentMarker.Brush, 0 );
            m_view.setData( newLayer );
            
	    	if( m_isMainCall ) {
	    		System.out.printf(
	    			"Image: outlined by stroke size of [%s] pixel\n",
	    		expand );
	    		m_fileName.replace( ".png", "_drawOUTLINE.png" );
	    	}
	    }
	    
	    public void inlineImage( float strokeSize )
	    {
	    	int contract = (int)(strokeSize+0.5);
            for( int y=0; y < contract; ++y )for( int x=0; x < m_view.W; ++x ) {
            	drawPixel( x, y, m_color );
            	int opYps = m_view.H - (y+1);
            	drawPixel( x, opYps, m_color );
            }
            for( int y=0; y < m_view.H; ++y )for( int x=0; x < contract; ++x ) {
            	drawPixel( x, y, m_color );
            	int opiX = m_view.W - (x+1);
            	drawPixel( opiX, y, m_color );
            }
	    	if( m_isMainCall ) {
	    		System.out.printf("Image: inlined by stroke size of [%s] pixel\n", contract );
	    		m_fileName.replace(".png","_drawINLINE.png");
	    	}
	    }
	    
	    // roundCorners overloads;
	    public void roundCorners()
	    {
	        // upper-left:
	        cutOneCorner( 0, 0 );
	        // upper-right:
	        cutOneCorner( m_view.W, 0 );
	        // lower-right:
	        cutOneCorner( m_view.W, m_view.H );
	        // lower-left:
	        cutOneCorner( 0, m_view.H );
	        
	        // change the filename under which the image will be saved later.
	        m_fileName = m_fileName.replace(".png","_roundCorners.png");
	        
	        // Output some info if called from command-line
	        System.out.printf( "Image: corners rounded by [%f] pixel\n",m_cornerSize);
	    }
	    
	    public void roundCorners(float size)
	    {
	    	 // when called by giving a size parameter, 
	    	// rebuild a new sized quarter-circle mask!
            if(size>0)
                calcCirquad(size);
	        // then round the corners:
	        roundCorners();
	    }
	    
	    public void roundCorners( float top, float bottom )
	    {
	    	  // when called by giving two size parameter, 
	    	 // rebuild a new sized quarter-circle mask 
            // for Top-corners and Bottom-corners each!
            if( top != bottom )
            {
	            // upper corners:
                if ( top > 0 )
                {
                    calcCirquad( top );
                    cutOneCorner( 0, 0 );
                    cutOneCorner( m_view.W, 0 );
                    // Output some info if called from command-line
                    System.out.printf("Image: rounded top corners by [%f] pixel\n", m_cornerSize);
                }
                
                // lower corners:
                if ( bottom > 0 )
                {
                    calcCirquad( bottom );
                    cutOneCorner( 0, m_view.H );
                    cutOneCorner( m_view.W, m_view.H );
                    // Output some info if called from command-line
                    System.out.printf("Image: rounded bottom corners by [%f] pixel\n", m_cornerSize);
                }
                
                // change the filename under which the image will be saved later.
	            m_fileName = m_fileName.replace( ".png", "_roundCorners.png" );
            }
            else // if top and bottom equal, call the one parameter overload 
            	roundCorners( top );
	    }

	    // cuttenEdges overloads:
	    public void cuttenEdges(int border) {
	    	cuttenEdges( border, border, border, border );
	    }
	    
	    public void cuttenEdges(int l,int t,int r,int b) {
	    	m_edgeLeft=l;
	    	m_edgeTop=t;
	    	m_edgeRight=r;
	    	m_edgeBottom=b;
	    	cuttenEdges();
	    }
	       
	    public void cuttenEdges()
	    {
	        m_view.X = m_edgeLeft;
	        m_view.Y = m_edgeTop;
	        m_view.W -= (m_edgeLeft+m_edgeRight);
	        m_view.H -= (m_edgeTop+m_edgeBottom);

	        Layer32 neu = Layer32.createLayer( m_view.W, m_view.H, m_view.format() );
	        
	        Layer32 cut = neu.letsGo( Layer32.Forwards );
	        while( cut.nextStep() ) {
	        	cut.setPixel( m_view.getPixel( cut.PointX(), cut.PointY() ).farb );
	        }
	        
	        m_view.X = m_view.Y = 0;
	        if ( CurrentMarker.Brush != null )
	        	neu.addLayer( CurrentMarker.Brush, 0 );
	        m_view.setData( neu );

	        if( m_isMainCall ) { 
	            // change the filename under which the image will be saved later.
	            m_fileName = m_fileName.replace(".png","_cuttenEdges.png");
	            // output info when called by commandline
	            System.out.print( "Image: cutten edges: [left: "+m_edgeLeft+
	                              "], [top: "+m_edgeTop+
	                              "], [right: "+m_edgeRight+
	                              "], [bottom: "+m_edgeBottom+"]\n" );
	        }
	    }
	    
	     // Will cut off edges by defining the area to keep, 
	    // instead by defining edges to remove.
	    public void cropFrame(int X,int Y,int W,int H)
	    {
	    	if (!(X>=0 && Y>=0
	        && (X+W) <= m_view.W
	        && (Y+H) <= m_view.H)) {
		    	X = X < 0 ? 0 : X;
		    	Y = Y < 0 ? 0 : Y;
		    	W = (X+W > m_view.W) 
		    	  ? m_view.W - X : W; 
		    	H = (Y+H > m_view.H) 
		    	  ? m_view.H - Y : H;
		    	System.err.print( "Imaging: WARNING - crop frame exceeds image bounds.\n" );
	    	}
	    	m_edgeLeft = X;
	    	m_edgeTop = Y;
	    	m_edgeRight = m_view.W - (X+W);
	    	m_edgeBottom = m_view.H - (Y+H);

	    	boolean callstate = nextIsSubCall( true );
	    		cuttenEdges();
	    	nextIsSubCall( callstate );
	    	
	    	if( m_isMainCall ) {
		        m_fileName = m_fileName.replace(".png","_cropFrame.png");
		        System.out.println( String.format(
		    	    "Image: cropped content within [%s %s %s %s]",
		    	                           X,Y,W,H )
		       		                 );
	    	}
	    }
	    
	    public void drawMarker( MarkerParameter parameters ) 
	    {
	    	CurrentMarker.Shape = parameters.Shape;
	    	CurrentMarker.Areal = parameters.Areal;
	    	CurrentMarker.Brush = parameters.Brush;
	    	setMarkerColor(       parameters.Color);
	    	drawMarker();
	    }
	    
	    public void drawMarker(SHAPE shape)
	    {
	        CurrentMarker.Shape = shape;
	        drawMarker();
	    }
	   
	    public void drawMarker()
            {
                boolean undefined;
                do {
                    undefined = false;

                    switch (CurrentMarker.Shape) {
                        case CIRCLE:
                        case ELLYPS:
                            CurrentMarker.Shape =( CurrentMarker.Areal.width
                            		            == CurrentMarker.Areal.height ) 
                                                ? SHAPE.CIRCLE 
                                                : SHAPE.ELLYPS;
                            drawCircle();
                            break;
                        case SQUARE:
                            drawSquare();
                            break;
                        case ARROW:
                        	drawArrow();
                        	break;
                        case CROSS:
                    		drawCrossHair( CurrentMarker.Areal.x,
                    				       CurrentMarker.Areal.y,
                    				       CurrentMarker.Areal.width );
                    		break;
                        case UNKNOWN:
                            undefined = true;
                        default:
                            CurrentMarker.Shape = SHAPE.CIRCLE;
                            break;
                    }
                } while (undefined);
            
            if(CurrentMarker.Shape!=SHAPE.ARROW) {   
	        System.out.print( "Image: add marker '" + CurrentMarker.Shape.toString()
	                   + "' at: x:" + CurrentMarker.Areal.x
	                        + " y:" + CurrentMarker.Areal.y 
	                        + " w:" + CurrentMarker.Areal.width
	                        + " h:" + CurrentMarker.Areal.height+"\n" );
            } m_fileName = m_fileName.replace(".png","_draw"+CurrentMarker.Shape.toString()+".png");
            CurrentMarker.Color.assign( m_color );
            m_lastMarker.assign( CurrentMarker ); 
	    }
	    
	    public void setMarkerColor( Color32 newColor )
	    {
	    	if( CurrentMarker.Color.getValue() != newColor.getValue() ) {
	    		m_color.assign( newColor );
	    		createBrush( m_dotHalbSize * 2, 1.0f );
	    		if( m_isMainCall ) {
	    			System.out.printf( "Image: set Color to: ["+m_color.toString()+"]\n" );
	    			CurrentMarker.Color.assign( newColor );
	    		}
	    	} 
	    }
	    
	    public void showObject( int iX, int Yps ) 
	    {
	    	Point pos = new Point(iX,Yps);
	    	if( CurrentMarker.Shape != SHAPE.ARROW ) {
	    		m_color.assign( CurrentMarker.Color.getAccentPrecise() );
	    		if( m_lastMarker.Shape == SHAPE.SQUARE ) {
	    			if( m_lastMarker.Areal.contains(pos) ) {
	    				pos.x = (m_lastMarker.Areal.x+(m_lastMarker.Areal.width/2));
	    			    pos.y = (m_lastMarker.Areal.y+(m_lastMarker.Areal.height/2));
	    			}
	    		} else {
	    			if( m_lastMarker.Areal.contains( pos.x+(m_lastMarker.Areal.width/2),
	    					                         pos.y+(m_lastMarker.Areal.height/2)) ){
	    				pos.x = m_lastMarker.Areal.x;
	    				pos.y = m_lastMarker.Areal.y;
	    			}
	    		}
	    	} drawArrow(pos.x,pos.y);
	    	m_color.assign( CurrentMarker.Color );
	    }
	    
	    public void showObject( SHAPE shape )
	    {
	    	if(shape!=SHAPE.ARROW) {
	    		drawMarker(shape);
	    		m_lastMarker.assign( shape, CurrentMarker.Areal,
	    			   CurrentMarker.Color, CurrentMarker.Lines );
	    	} showObject( CurrentMarker.Areal.x,
	    			      CurrentMarker.Areal.y );
	    }
	    
	    public void dragObject( @Struct.Point long grab, @Struct.Point long drop )
	    {
	    	int r = (int)(Struct.distance(grab,drop)/10.0f);
			CurrentMarker.Areal.width = 
			CurrentMarker.Areal.height = r;
			CurrentMarker.Areal.x = Struct.getX(grab);
			CurrentMarker.Areal.y = Struct.getY(grab);
			CurrentMarker.Color.assign( m_color );
			try{ m_color.assign("soft"); } catch(Color32.UnknownColor ex) {};
			m_color.assign( m_color.layOverRGBA( 
				CurrentMarker.Color.valueOf( 
							CurrentMarker.Color.getInverseMultiplied() 
							                 )    ) 
					          );
			m_color.setAlpha( CurrentMarker.Color.getAlpha()/2 );
	    	boolean currentstate = nextIsSubCall(true);
	    		setMarkerColor( m_color );
				drawCircle();
				m_color.assign( CurrentMarker.Color );
	    		createBrush( m_dotHalbSize*2, 1.0f );
				drawArrow( CurrentMarker.Areal.x,
						   CurrentMarker.Areal.y,
						   Struct.getX(drop),
						   Struct.getY(drop) );
				m_color.setRGBA( CurrentMarker.Color.getInverseMultiplied(),
						  (int)( CurrentMarker.Color.getAlpha()*0.8f) );
				setMarkerColor(m_color);
				drawCrossHair( Struct.getX(drop),
						       Struct.getY(drop),
						       r );
				m_color.assign( CurrentMarker.Color );
	    		createBrush( m_dotHalbSize*2, 1.0f );
			nextIsSubCall(currentstate);
			if( m_isMainCall ) {
				System.out.printf("Image: visulized drag from [%s:%s] to [%s:%s]\n",
						           Struct.getX(grab),Struct.getY(grab),
						           Struct.getX(drop),Struct.getY(drop));
				m_fileName = m_fileName.replace(".png","_dragObject.png"); 
			}
	    }
	    
	    // flip-rotate the image counter-clock-wise by 90degree-flipsteps 
	    public void counterClock( int steps )
	    {
	    	int[] rotatedImage=null; 
   		    int oldW = m_view.layer.Width();
   		    int oldH = m_view.layer.Height();
	    	int difX = oldW-1;
    		int difY = oldH-1;
   		    int newW = oldH;
   		    int newH = oldW;
   		    
    		if( steps >= 90 || steps <= -90 )
    			steps/=90;
   			steps = steps < 0
   				  ? -(steps % 4)
   			      : (4 - steps % 4) % 4;

   		    rotatedImage = new int[ Struct.indices( m_view.layer.Size() ) ];
	    	switch(steps) 
	    	{
	    	case 1: {
	    		for( int y=0; y<newH; ++y )
	    			for( int x=0; x<newW; ++x ) {
	    				int X = difX - y;
	    				int Y = x; // difY - x;
	    				rotatedImage[y*newW+x] = m_view.layer.getPixel(X,Y).farb;
	    			}
	    	} break;
	    	case 2: {
	   		    newW = oldW;
	   		    newH = oldH;
	    		for(int y=0;y<newH;++y)
	    			for(int x=0;x<newW;++x) {
	    				int X = difX - x;
	    				int Y = difY - y;
	    				rotatedImage[y*newW+x] = m_view.layer.getPixel(X,Y).farb;
	    			}
	    	} break;
	    	case 3: {
	    		for(int y=0;y<newH;++y)
	    			for(int x=0;x<newW;++x) {
	    				int X = y; // difX - y;
	    				int Y = difY - x;
	    				rotatedImage[y*newW+x] = m_view.layer.getPixel(X,Y).farb;
	    			}
	    	} break;
	    	}
	    	
	    	if(steps!=0) {
	    		m_view.layer.moveLayer( 0, 0 );
	    		m_view.layer.Size( newW, newH );
	    		try { m_view.layer.setData( rotatedImage );
	    		} catch( ArrayIndexOutOfBoundsException ex ) {
	    			System.err.println("Error: "+ex.getMessage());
	    		} steps = 360 + (steps * -90);
	    		m_view.W = m_view.layer.Width();
	    		m_view.H = m_view.layer.Height();
	    		m_fileName = m_fileName.replace(".png","_rotated"+steps+".png");
	    		System.out.print("Image: applied rotation of ["+steps+"] degree\n");
	    	}
	    }
	    
	    public void flipImage( DIRECTION direction )
	    { 
    		int[] flipedimage = new int[ Struct.indices( m_view.Size() ) ];
    		switch(direction)
    		{
    		case UP:
    			int Y = m_view.H;
    			for( int y=0; y<m_view.H; ++y ) { --Y;
    				for( int x=0; x<m_view.W; ++x ) {
    					flipedimage[y*m_view.W+x] = m_view.layer.getPixel(x,Y).farb;
    				}
    			} break;
    		case RIGHT:
    			for( int y=0; y<m_view.H; ++y ) {int X = m_view.W;
    				for( int x=0; x<m_view.W; ++x ) { --X;
    					flipedimage[y*m_view.W+x] = m_view.layer.getPixel(X,y).farb;
    				}
    			} break;
    		}
    		try {
    			m_view.layer.moveLayer(0,0);
    			m_view.layer.setData( flipedimage );
    		} catch( ArrayIndexOutOfBoundsException ex ) {
    			System.err.print("Error: "+ex.getMessage()+"\n");
    		}
    		m_fileName = m_fileName.replace(".png","_fliped-"+direction.toString()+".png");
    		System.out.print("Image: flipped ["+direction.toString()+"]!\n");
	    }
	    
	    public void saveImageFile(String filePath)
	    { 
	    	if ( m_image.width != m_view.W || m_image.height != m_view.H ) {
	    		int imgtype = m_image.type;
	    		m_image = new ImageData( m_view.layer.Width(), m_view.layer.Height(),
	    				                 m_image.depth, m_image.palette );
	    		m_image.type = imgtype;
	    		checkAlphaChannelData( m_image );
	    	}
	    	Layer32 img = m_view.layer.letsGo( Layer32.Forwards );
	    	while ( img.nextStep() ) {
	    		m_image.setPixel( img.PointX(), img.PointY(), img.getRGB() );
	    		m_image.alphaData[img.Index()] = (byte)img.getAlpha();
	    	}
	        m_loader.data[0] = m_image;
	        FileOutputStream fOut=null;
	        
	        try {
	            fOut = new FileOutputStream(filePath);
	            m_loader.save( fOut, SWT.IMAGE_PNG );
	            fOut.flush();
	            fOut.close();
	            System.out.print( "Image: saved as: "+filePath+"\n" );
	        } catch(Exception fehler) {
	            System.err.printf( "Image: "+fehler.getMessage() );
	            System.out.printf( "Image: error saving file: %s", filePath );
	        }
	    }

	    public void saveImageFile(boolean unrenamed) {
	    	saveImageFile( unrenamed ? m_originalName : m_fileName );
	    }
	    
	    public void saveImageFile() {
	    	saveImageFile(false);
	    }

	    public MarkerParameter copyCurrentState() {
	    	MarkerParameter copy = new MarkerParameter( CurrentMarker.Color.format() );
	    	copy.assign( CurrentMarker );
	    	return copy;
	    }
	    
		public String getImagePath( int layer ) {
			return m_fileName;
		}
		
	    public void setImagePath( String overrideName ) {
	    	m_originalName = overrideName;
	    }
	    
		public Layer32 getImageData( int layer ) {
			Layer32 l32 = m_view.layer;
			while( layer > 0 ) {
		    	l32 = l32.nextAbove();
		    } return l32;
            //m_loader.data[layer]; 
		}
		
}

