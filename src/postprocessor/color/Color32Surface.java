package postprocessor.color;

import java.lang.Integer;
import java.lang.Long;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.util.Map;

import postprocessor.color.Color32;
import postprocessor.color.Color32.UnknownColor;
import postprocessor.color.Color32.UnknownFormat;
import postprocessor.struct.*;

public final class Color32Surface
{
	public static enum FORMAT {
		RGBA, ARGB, BGRA, ABGR, GRAY, USER, INVALID
	}
	
	public static class SURFACECODE {
		public static final int RGBA = 0|1<<8|2<<16|3<<24;
		public static final int ARGB = 3|0<<8|1<<16|2<<24;
		public static final int BGRA = 2|1<<8|0<<16|3<<24;
		public static final int ABGR = 3|2<<8|1<<16|0<<24;
		public static final int GRAY = 1|1<<8|3<<16|3<<24;
		
		public final int    typecode;
		public final FORMAT format;
		
		private SURFACECODE( int surfacetypecode ) {
			switch( typecode = surfacetypecode ) {
			case 0: format=FORMAT.INVALID; break;
			case RGBA: format=FORMAT.RGBA; break;
			case ABGR: format=FORMAT.ABGR; break;
			case BGRA: format=FORMAT.BGRA; break;
			case ARGB: format=FORMAT.ARGB; break;
			case GRAY: format=FORMAT.GRAY; break;
			default: format = FORMAT.USER; break;
			}
		}
		
		public static SURFACECODE fromChannelOrder( String channelOrder )
			   throws UnknownFormat
		{
			int typecode = 0;
			if( channelOrder.length() != 4 )
				throw new UnknownFormat( channelOrder );
			for(int channel = 0; channel < 4; ++channel) {
				switch( channelOrder.charAt(channel) ) {
				case 'R':
				case 'r': typecode = typecode; break;
				case 'G':
				case 'g': typecode = (typecode | (1 << (channel*8))); break;
				case 'B':
				case 'b': typecode = (typecode | (2 << (channel*8))); break;
				case 'A':
				case 'a': typecode = (typecode | (3 << (channel*8))); break;
				default: 
					throw new UnknownFormat( channelOrder );
				}
			} return new SURFACECODE( typecode );
		}

		public SURFACECODE( FORMAT fCode ) {
			switch( format = fCode ) {
			case RGBA: typecode=RGBA; break;
			case ARGB: typecode=ARGB; break;
			case BGRA: typecode=BGRA; break;
			case ABGR: typecode=ABGR; break;
			case GRAY: typecode=GRAY; break;
			default: typecode = 0; break;
			} 
		}

		public FORMAT getFormat() {
			return format;
		}
		public int getCode() {
			return typecode;
		}
	}
	
	public static class Layout
	{
		public class Index {
			final public byte R,G,B,A;
			public Index(int r,int g, int b, int a) {
				R=(byte)r; G=(byte)g; B=(byte)b; A=(byte)a;
			}
		}
		public class Shift {
			final public byte R,G,B,A;
			public Shift(int r,int g, int b, int a) {
				R=(byte)r; G=(byte)g; B=(byte)b; A=(byte)a;
			}
		}
		public class Masks {
			final public int R,G,B,A;
			public Masks(int r,int g,int b,int a) {
				R=r; G=g; B=b; A=a;
			}
		}
		
		public final SURFACECODE surface;
		
		public final byte[] ALPHA;
		public final byte[] INDEX;
		public final byte[] SHIFT; 
		public final int[]  MASKS;

		public final Index  index;
		public final Shift  shift;
		public final Masks  masks;
		
		private final static Color32[] tempinstances = new Color32[] {
			new Color32.RGBA(),	new Color32.ARGB(),	new Color32.BGRA(),
			new Color32.ABGR(), new Color32.GRAY()
		};
		
		public Color32 getTempInstance() {
			return tempinstances[surface.format.ordinal()];
		}
		public Color32 getTempInstance(int rawcolorvalue)
		{
			Color32 val = tempinstances[surface.format.ordinal()];
			val.farb = rawcolorvalue;
			return val;
		}
		
		public Layout( FORMAT format )
		{
			this( new SURFACECODE( format ) );
		}
		
		public Layout( SURFACECODE type )
		{
			surface = type;
			index = new Index( (surface.typecode & 0x000000ff),     
					           (surface.typecode & 0x0000ff00)>>8,
						       (surface.typecode & 0x00ff0000)>>16,
						       (surface.typecode & 0xff000000)>>24 );
			shift = new Shift( index.R<<3, index.G<<3, index.B<<3, index.A<<3 );
			masks = new Masks( 0xff<<shift.R, 0xff<<shift.G, 0xff<<shift.B, 0xff<<shift.A );
			
			INDEX = new byte[4];
			INDEX[0] = index.R;
			INDEX[1] = index.G;
			INDEX[2] = index.B;
			INDEX[3] = index.A;
			
			SHIFT = new byte[4];
			SHIFT[index.R] = shift.R;
			SHIFT[index.G] = shift.G;
			SHIFT[index.B] = shift.B;
			SHIFT[index.A] = shift.A;
			
			MASKS = new int[4];
			MASKS[index.R] = masks.R;
			MASKS[index.G] = masks.G;
			MASKS[index.B] = masks.B;
			MASKS[index.A] = masks.A;
			
			ALPHA = new byte[4];
			ALPHA[index.R] = (byte)(index.R>index.A?index.R-1:index.R);
			ALPHA[index.G] = (byte)(index.G>index.A?index.G-1:index.G);
			ALPHA[index.B] = (byte)(index.B>index.A?index.B-1:index.B);
			ALPHA[index.A] = (byte)(3);
		}
		
		public int Compose( int r, int g, int b, int a ) {
	        return ((r << shift.R) 
	        	   |(g << shift.G) 
	        	   |(b << shift.B) 
	        	   |(a << shift.A));
	    }
		    
	    public int ConvertFrom( Layout that, int color ) {
	    	return ( (((color&masks.R)>>>this.shift.R)<<that.shift.R)
	    		   | (((color&masks.G)>>>this.shift.G)<<that.shift.G)
	    		   | (((color&masks.B)>>>this.shift.B)<<that.shift.B)
	    		   | (((color&masks.A)>>>this.shift.A)<<that.shift.A) );
	    }
	    
	    public int ConvertInto( Layout that, int color ) {
	    	return ( (((color&that.masks.R)>>>that.shift.R)<<shift.R)
	    		   | (((color&that.masks.G)>>>that.shift.G)<<shift.G)
	    		   | (((color&that.masks.B)>>>that.shift.B)<<shift.B)
	    		   | (((color&that.masks.A)>>>that.shift.A)<<shift.A) );
	    }
	    
	    public int AlphaRemove( int vier ) {	    		
	    	return (((vier & masks.R) >>> shift.R) << SHIFT[ALPHA[index.R]])
	    		 | (((vier & masks.G) >>> shift.G) << SHIFT[ALPHA[index.G]])
	    		 | (((vier & masks.B) >>> shift.B) << SHIFT[ALPHA[index.B]]);
	    }
	    
	    public int AlphaInsert( int drei, int eins ) {
	    	return (((drei & MASKS[ALPHA[index.R]]) >>> SHIFT[ALPHA[index.R]]) << SHIFT[index.R])
		    	 | (((drei & MASKS[ALPHA[index.G]]) >>> SHIFT[ALPHA[index.G]]) << SHIFT[index.G])
		    	 | (((drei & MASKS[ALPHA[index.B]]) >>> SHIFT[ALPHA[index.B]]) << SHIFT[index.B])
		    	 | eins << shift.A;
	    }
	    
	    public int multiplied( int color ) {
	    	int multipilz = (color & masks.A) >>> shift.A;
	    	return (((((color & masks.R)>>>shift.R) * multipilz) / 255) << shift.R) 
	    	     | (((((color & masks.G)>>>shift.G) * multipilz) / 255) << shift.G) 
	    	     | (((((color & masks.B)>>>shift.B) * multipilz) / 255) << shift.B);
	    }
	    public int multiplied( int color, int antiprop ) { 
	    	  // antipilz is assumed being external value which's used 'OneMinus'ed here for multiplier
	    	 // e.g. for multiplying by other layer's alpha at pixel position above or beneath (Z-axis) this pixel contained layer    
	    	int multipilz = ((color & masks.A) >>> shift.A) * antiprop;
            return (((((color & masks.R)>>>shift.R) * multipilz ) / 65025 ) << shift.R)
                 | (((((color & masks.G)>>>shift.G) * multipilz ) / 65025 ) << shift.G)
                 | (((((color & masks.B)>>>shift.B) * multipilz ) / 65025 ) << shift.B);
	    }
                
	    public int invert( int color ) {
	    	return ( masks.R - ( color & masks.R ) )
	    		 | ( masks.G - ( color & masks.G ) )
	    		 | ( masks.B - ( color & masks.B ) )
	    		 | ( color & masks.A );
	    }
	     
	    // parse a color from either a named color or from a
	    // (hexadecimal) color value's string representation
	    // ... OR .. applies a modifier to some additional 
	    // color value which was given ('.' separated) also 
	    // valid modifiers are:  'Deep', 'Dark', 'Soft', 'Light'
	    // 'ACCENT', 'INVERS' etc... So Dark.Green would be
	    // equivalent to calling Compose("Dark",Compose("Green"))
	    // which would return a darkened variant of the default
	    // Green color value. throws 'UnknownColor' exception
	    // on unknown, not resolvable input strings passed. 
	    public int Compose( String colorname, int value ) throws UnknownColor {
	    	char checker = colorname.charAt(1);
	    	if( checker=='x'||checker=='X' ) {
	    		if(colorname.length()>=10)
	    			return Long.decode(colorname).intValue();
	    		else if(colorname.length()>=8)
	    			return AlphaInsert(Integer.decode(colorname),255);
	    		else throw new UnknownColor(colorname);
	    	} checker = colorname.charAt(0);
	    	if( checker>96 ) {
	    		int len = colorname.length();
	    		char[] clrnam = new char[len];
	    		colorname.getChars(1,len,clrnam,1);
	    		clrnam[0]=(char)(checker-32);
	    		colorname = String.copyValueOf(clrnam);
	    	} 
	    	if(colorname.contains(".")) {
	    		String[] variant = colorname.split("[.]");
	    		Color32 mixer = getTempInstance().clone();
	    		int color = 0;
	    		for(int i=variant.length-1;i>=0;--i) {
	    			mixer.assign( Compose( variant[i], color ) );
	    			if (color != 0) { 
	    				int a = mixer.getAlpha()/2;
	    				if(variant[i].charAt(0)>96)
	    					a /= 2;
	    				color = mixer.setA(a).layOverRGBA(getTempInstance(color).setA(255-a));
	    			} else color = mixer.farb;
	    		} return color;
	    	}

	    	switch(colorname) {
	    	case "Red": return Compose(255,0,0,255);
	    	case "Green": return Compose(0,255,0,255);
	    	case "Blue": return Compose(0,0,255,255);
	    	case "Yellow":
	    	case "Gelb": return Compose(255,255,0,255);
	    	case "Cyan": return Compose(0,255,255,255);
	    	case "Magenta": return Compose(255,0,255,255);
	    	case "Orange": return Compose(255,128,0,255);
	    	case "Mint": return Compose(0,255,128,255);
	    	case "Pink": return Compose(255,0,128,255);
	    	case "Black": return Compose(0,0,0,255);
	    	case "Gray":
	    	case "Grey": return Compose(127,127,127,255);
	    	case "Soft": return Compose(160,160,160,160);
	    	case "Wheat": return Compose(224,180,200,255);
	    	case "White": return Compose(255,255,255,255);
	    	case "Lite": return Compose(220,220,220,100);
	    	case "Bright": return Compose(224,220,208,100);
	    	case "Deep": { int rgb = AlphaRemove(value); int a = (value&masks.A>>shift.A); a += (255-a/4); a = a>255?255:a; 
	    		           return Compose((rgb&masks.R)>>shift.R,(rgb&masks.G)>>shift.G,(rgb&masks.B)>>shift.B,a);	}
	    	case "Dark": return getTempInstance(Compose(32,32,32,200)).layOverRGB(AlphaRemove(value))|(value&masks.A);
	    	case "Light": return getTempInstance(Compose(220,220,220,200)).layOverRGB(AlphaRemove(value))|(value&masks.A);
	    	case "Cold": return getTempInstance(Compose(160,196,255,32)).layOverRGB(value)|(value&masks.A);
	    	case "Nolled": return 0&value; // <<-- 'Noll' is transparent black, but dominant within mixing against any other color. (where transparent black would be LESS dominant then any other colors)
	    	case "Invert": return invert(multiplied(value)|(value&masks.A));
	    	case "Accent": return getTempInstance(value).getAccentPrecise();
	    	case "Opaque": return ((value&~masks.A)|255<<shift.A);
	    	default: throw new UnknownColor(colorname);
	    	
	    	}
	    }
	};
	
	public static class RGBA extends Layout {
		private static Color32.RGBA TempInstance = new Color32.RGBA();
		public RGBA() {
			super(FORMAT.RGBA);
		}
		@Override
		public Color32 getTempInstance() {
			return TempInstance;
		}
		@Override
		public Color32 getTempInstance(int farbval) {
			TempInstance.farb = farbval;
			return TempInstance;
		}
	};
	
	public static class ABGR extends Layout {
		private static Color32.ABGR TempInstance = new Color32.ABGR();
		public ABGR() {
			super(FORMAT.ABGR);
		}
		@Override
		public Color32 getTempInstance() {
			return TempInstance;
		}
		@Override
		public Color32 getTempInstance(int farbval) {
			TempInstance.farb = farbval;
			return TempInstance;
		}
	};
	
	public static class ARGB extends Layout {
		private static Color32.ARGB TempInstance = new Color32.ARGB();
		public ARGB() {
			super(FORMAT.ARGB);
		}
		@Override
		public Color32 getTempInstance() {
			return TempInstance;
		}
		@Override
		public Color32 getTempInstance(int farbval) {
			TempInstance.farb = farbval;
			return TempInstance;
		}
	};
	
	public static class BGRA extends Layout {
		private static Color32.BGRA TempInstance = new Color32.BGRA();
		public BGRA() {
			super(FORMAT.BGRA);
		}
		public Color32 getTempInstance() {
			return TempInstance;
		}
		public Color32 getTempInstance(int farbval) {
			TempInstance.farb = farbval;
			return TempInstance;
		}
	};
	
	public static class GRAY extends Layout {
		private static Color32.GRAY TempInstance = new Color32.GRAY();
		public GRAY() {
			super(FORMAT.GRAY);
		}
		public Color32 getTempInstance() {
			return TempInstance;
		}
		public Color32 getTempInstance(int farbval) {
			TempInstance.farb = farbval;
			return TempInstance;
		}
		@Override
		public int AlphaInsert( int zweiGray, int zweiAlfs ) {
			return -1;
		}
	};
	
};