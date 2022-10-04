package postprocessor.color;

import postprocessor.color.Color32.UnknownColor;
import postprocessor.color.Color32Surface.*;


abstract public class Color32 extends Number
{
  ///////////////////////////////////////////////////////
  // exceptions:
	
	public static class UnknownColor extends Exception	{
		public UnknownColor(String unknownName) {
			super(String.format("'%s' is neither colorname nor hexvalue.\n",unknownName));
		}
		public UnknownColor(Color32Surface.SURFACECODE unknownFormat) {
			super( String.format( "Color32Surface type: '%s' - '%s'.\n",
				   Integer.valueOf(unknownFormat.typecode).toString(),
				   unknownFormat.format.toString() )
			);
		}
	}
	public static class UnknownFormat extends Exception	{
		public UnknownFormat(String invalidFormat) {
			super(String.format("Invalid Color32Surface format: '%s'.\n",invalidFormat));
		}
		public UnknownFormat(Color32Surface.FORMAT unknownFormat) {
			super(String.format("Color32Surface: '%s'.\n",unknownFormat.toString()));
		}
	}
	
  ////////////////////////////////////////////////////
  // abstracts:
	
	// get a color32 instance's concrete channel configuration 
	abstract protected Layout c();
	// create an identical clone by same color of same format
	abstract public Color32 clone();
	
  ////////////////////////////////////////////////////
  // concrete implementations:
	
	public static class GRAY extends Color32
	{
        protected final static Color32Surface.GRAY grayface = new Color32Surface.GRAY();
		@Override
		protected Layout c() {
			return grayface;
		}

		@Override
		public Color32 clone() {
			Color32 gray = Color32.createColor( grayface.surface.format );
			gray.farb = this.farb;
			return gray;
		}

		public short[] getShorts() {
			byte[] supers = super.getChannels();
			return new short[] {
				(short)(supers[0]|supers[1]<<8),
				(short)(supers[2]>>16|supers[3]>>>16)
		    };
		}

		// from GRAY surface, getAlpha() returns unsigned short (16bit) value of the alpha channel (range 0-65535) 
		@Override
		public short getAlpha() {
			return (short)((int)(farb & grayface.masks.A & grayface.masks.B)>>grayface.shift.G);
		}
		// on a GRAY surface, setAlpha() takes a 16bit (ushort) parameter (directly sets the pixels 16bit alpha value) 
		@Override
		public void setAlpha( int alphaShort ) {
			farb = (farb & grayface.masks.R&grayface.masks.G)|(alphaShort<<grayface.shift.B);
		}

		// from GRAY surface getRGB() returns unsigned short (16bit) value of the GRAY channel (range 0-65535)
		@Override
		public int getRGB() {
			return (int)(farb&grayface.masks.R&grayface.masks.G)<<grayface.shift.B;
		}
		// on a GRAY surface, setRGB(ushort) directly sets the (16bit) Luma value of the pixel (which has range 0-65535)
		@Override 
		public void setRGB(int grayShort) {
			farb = (grayShort<<grayface.shift.R)|(farb&grayface.masks.A&grayface.masks.B);
		}
		
		public float getLuma() {
			return (float)getRGB()/65535;
		}
		public Color32 setLuma( float luma ) {
			if ( luma > 1.0f ) setRGB( (int)luma );
			else setRGB( (int)(65535*luma) );
			return this;
		}

		@Override
		public short getR() {
			return (short)(getRGB()>>>8);
		}
		@Override
		public short getG() {
			return (short)(getRGB()>>>8);
		}
		@Override
		public short getB() {
			return (short)(getRGB()>>>8);
		}
		
		// on a GRAY surface, setR(byte) effects change of Luma channel the same way, as it
		// would change Luma on also RGB format surfaces when passed to as red channel 
		@Override
		public Color32 setR( int redByte ) {
			setG(((redByte*256)+(2*getG()))/3);
			return this;
		}
		@Override
		public Color32 setG( int greenByte ) {
			setG(((greenByte*256)+(2*getG()))/3);
			return this;
		}
		// on a GRAY surface, setB(byte) effects same degree on Luma change to the gray surface, as also that same
		// value would effect to a piixel's Luma value if that pixel would be an RGB/RGBA surface formated pixel 
		@Override
		public Color32 setB( int blueByte ) {
			setG(((blueByte*256)+(2*getG()))/3);
			return this;
		}
		// on a GRAY surface, setA(ushort) sets the 16bit Alpha value from given (byte) parameter which is scaled up (by 256) to match a 16bit range 
		@Override
		public Color32 setA( int alphaByte ) {
			setAlpha( alphaByte * 256 );
			return this;
		}
		

		@Override
		public void assign( Color32 other ) {
			assignChecked( other );
		}

		@Override
		public void assign( long colorValue ) {
			farb = (int)colorValue;
		}

		@Override
		public Color32 assign( String namedColor ) throws UnknownColor {
			return super.assign( namedColor );
		}
	    @Override
		public void assignChecked( Color32 other ) {
	    	if ( this.grayface.surface.typecode != other.c().surface.typecode ) {
	    		setLuma( ( ( other.getR() + other.getG() + other.getB() )*256 ) / 3 )
	    	   .setAlpha( other.getAlpha() * 256 );
	    	} else {
	    		this.farb = other.farb;
	    	}
	    }
	    
		@Override
		public Color32 Compose( int gray, int alpha ) {
			return clone().setLuma( gray ).setA( alpha );
		}

		@Override
		public short shortValue() {
			return (short)getRGB();
		}

		public GRAY() {
			super();
		}

		public GRAY( int g, int a ) {
			super();
			setG( g ).setAlpha( a );
		}

		public GRAY( int value ) {
			super();
			setG( value );
		}

		public GRAY( String namedColor ) throws UnknownColor {
			super(); farb = -1;
			assignChecked( RGBA.ValueOf( RGBA.layout.Compose( namedColor, farb ) ) );
		}
		
	}
	
	public static class RGBA extends Color32
	{
		protected final static Color32Surface.RGBA layout = new Color32Surface.RGBA();
		@Override
		protected Layout c() {
			return layout;
		}
		@Override
		public Color32 clone() {
			return new RGBA(this.farb);
		}
		public static Color32 ValueOf(int value) {
	    	return layout.getTempInstance(value);
	    }
		
		public RGBA() {
			super();
		}
		public RGBA(int rgbaValue) {
			super(rgbaValue);
		}
		public RGBA(int rgb, int a) {
			super(rgb,a);
		}
		public RGBA(int r, int g, int b) {
			super(r,g,b,255);
		}
		public RGBA(int r, int g, int b,int a) {
			super(r,g,b,a);
		}
		public RGBA(String namedColor) throws UnknownColor {
			super(namedColor);
		}
	}
	public static class ARGB extends Color32
	{
		public final static Color32Surface.ARGB layout = new Color32Surface.ARGB();
		@Override
		protected Layout c() {
			return layout;
		}
		@Override
		public Color32 clone() {
			return new ARGB(this.farb);
		}
		public static Color32 ValueOf(int value) {
	    	return layout.getTempInstance(value);
	    }
		
		public ARGB() {
			super();
		}
		public ARGB(int argbValue) {
			super(argbValue);
		}
		public ARGB(int rgb, int a) {
			super(rgb,a);
		}
		public ARGB(int r, int g, int b) {
			super(r,g,b,255);
		}
		public ARGB(int r, int g, int b,int a) {
			super(r,g,b,a);
		}
		public ARGB(String namedColor) throws UnknownColor {
			super(namedColor);
		}
	}
	public static class BGRA extends Color32
	{
		public final static Color32Surface.BGRA layout = new Color32Surface.BGRA();
		@Override
		protected Layout c() {
			return layout;
		}
		@Override
		public Color32 clone() {
			return new BGRA(this.farb);
		}
		public static Color32 ValueOf(int value) {
	    	return layout.getTempInstance(value);
	    }
		
		public BGRA() {
			super();
		}
		public BGRA(int BGRAvalue) {
			super(BGRAvalue);
		}
		public BGRA(int rgb, int a) {
			super(rgb,a);
		}
		public BGRA(int r, int g, int b) {
			super(r,g,b,255);
		}
		public BGRA(int r, int g, int b,int a) {
			super(r,g,b,a);
		}
		public BGRA(String namedColor) throws UnknownColor {
			super(namedColor);
		}
	}
	public static class ABGR extends Color32
	{
		public final static Color32Surface.ABGR layout = new Color32Surface.ABGR();
		@Override
		protected Layout c() {
			return layout;
		}
		@Override
		public Color32 clone() {
			return new ARGB(this.farb);
		}
		public static Color32 ValueOf(int value) {
	    	return layout.getTempInstance(value);
	    }
		
		public ABGR() {
			super();
		}
		public ABGR(int ABGRval) {
			super(ABGRval);
		}
		public ABGR(int rgb, int a) {
			super(rgb,a);
		}
		public ABGR(int r, int g, int b) {
			super(r,g,b,255);
		}
		public ABGR(int r, int g, int b,int a) {
			super(r,g,b,a);
		}
		public ABGR(String namedColor) throws UnknownColor {
			super(namedColor);
		}
	}
	
  ///////////////////////////////////////////////////////////////////////////////////
  // static constructors which select concrete implementation by FORMAT parameter  
	
	public static Color32 createColor( Color32Surface.FORMAT formatcode ) 
	{ switch(formatcode){
		case RGBA: return new RGBA();
		case ABGR: return new ABGR();
		case BGRA: return new BGRA();
		case ARGB: return new ARGB();
		default: return null;
	}}
	
	public static Color32 createColor( Color32Surface.FORMAT formatcode, int rgb, int a ) 
	{ switch(formatcode){
		case RGBA: return new RGBA(rgb,a);
		case ABGR: return new ABGR(rgb,a);
		case BGRA: return new BGRA(rgb,a);
		case ARGB: return new ARGB(rgb,a);
		default: return null;
	}}
	
	public static Color32 createColor( Color32Surface.FORMAT formatcode, int r, int g, int b, int a ) 
	{ switch(formatcode){
		case RGBA: return new RGBA(r,g,b,a);
		case ABGR: return new ABGR(r,g,b,a);
		case BGRA: return new BGRA(r,g,b,a);
		case ARGB: return new ARGB(r,g,b,a);
		default: return null;
	}}
	
  ////////////////////////////////////////////////////
  // the color value which this pixel's color allocates:
	public int farb;
	
  ////////////////////////////////////////////////////
  // temporary objects:
	public Color32 valueOf( int value ) {
    	return c().getTempInstance( value );
    }
	
	public Color32 valueOf( int value, int alpha ) {
		return c().getTempInstance( c().AlphaInsert(value,alpha) );
	}
	

  ///////////////////////////////////////////////////////
  // interface: 'java.lang.Number'

	@Override
	public int intValue() {
		return farb;
	}
	@Override
	public long longValue() {
		return farb;
	}
	// casting to float makes a pixel returning it's 
	// 'Luma' (0 to 1.0) instead of it's color value
	@Override
	public float floatValue() {
		return (float)( ( getR() + getG() + getB() ) * getAlpha() ) / 195075.0f;
	}
	@Override
	public double doubleValue() {
		return (double)( ( getR() + getG() + getB() ) * getAlpha() ) / 195075.0;
	}
	public Number asNumber()
	{
		return this;
	}
	public Integer asInteger()
	{
		return (Integer)asNumber();
	}

  /////////////////////////////////////////////////////////////
  // virtual constructors:	

    // construct an empty ('invisible') Color32 pixel
	protected Color32() {
        farb = 0;
    }
    // create a color32 instance of rgb parameter by shifting
	// alpha parameter at it's formats correct alpha's shift
	// position in between the given rgb triplet bytes.
    protected Color32( int rgb, int a ) {
    	farb = c().AlphaInsert(rgb,a);
    }
    // color32value is assumed coming at correct byte order for
    // matching the constructed instance's surface format.
    protected Color32( int value ) {
        farb = value;
    }
    // construct an opaque pixel. (alpha implies to 255)
    protected Color32( short r, short g, short b ) {
        farb = c().Compose(r, g, b, 255);
    }
    // construct a 4channel color by given channel values 
    protected Color32(int r, int g, int b, int a) {
        farb = c().Compose(r, g, b, a);
    }
    // construct a color by it's name ('red','Orange','pink') or even by a hexadecimal
    // value's string representation (like: '0xffe5a288' or, without alpha: '0xffe5a2') 
    protected Color32( String namedColor ) throws UnknownColor {
    	farb = c().Compose( namedColor, 0 ); 
    }
    
  ////////////////////////////////////////////////////////////////////////////////////////
  // getter functions
    
    public Color32Surface.FORMAT format()
    {
    	return c().surface.format;
    }
    public Color32Surface.Layout layout()
    {
    	return c();
    }
    public byte[] getChannels() {
    	return new byte[]{ (byte)getChannel(0), (byte)getChannel(1), (byte)getChannel(2), (byte)getChannel(3) };
    }
    
    public short getChannel(int channelIndex) {
    	return (short)((farb & c().MASKS[channelIndex])>>>c().SHIFT[channelIndex]);
    }

    public int Compound(int channelIndex) { 
        return farb & c().MASKS[channelIndex];
    }

    public int getRGB() {
    	return c().AlphaRemove( farb );
    }

    // shortcuts for calling "getChannel(chan.Index)"...
    public short getAlpha(){
    	return getChannel(c().index.A);
    }
    public float getLuma() {
    	return this.floatValue();
    }
    public short getR() {
    	return getChannel(c().index.R);
    }
    public short getG() {
    	return getChannel(c().index.G);
    }
    public short getB() {
    	return getChannel(c().index.B);
    }
    // get the 'raw' color value by surface format depending byte order as is.
    public int getValue() {
        return farb;
    }
    // get the 'raw' color value by surface format depending byte order as is. (equals calling getValue())
    public int getRGBA() {
    	return farb;
    } 
    
    // get the color value as is, but pre-multiplied by it's current (after multiplying set to 255) alpha value
    public int getMultiplied() {
    	return c().multiplied(farb);
    }

    // 'OneMinusAlpha' here for real means 255MinusAlpha  
    // since this color class uses 1byte channel ranges
    public int getMultiplied( int OneMinusAlpha ) {
    	return c().multiplied( farb, OneMinusAlpha );
    } 

    // return this color but with given color channel exchanged 
    public int withChannel( int chanIdx, int chanVal ) {
    	return ( farb & ~c().MASKS[chanIdx] ) | ( (chanVal%256) << c().SHIFT[chanIdx] );
    }
    // return this color but with alpha channel exchanged
    public int withAlpha( int alphaVal ) {
    	return ( farb & ~c().masks.A )|( (alphaVal%256) << c().shift.A );
    }
    
    // get this color's inverse value (or it's 'negative' color).
    // this effects changes to also Croma and Luma... (without changing alpha) 
    public int getInverseColor() {
    	return c().invert(farb);
    }
    // get this color's inverse value where first multiplying
    // alpha before doing inversion at least. (the lesser the
    // color's alpha value, the lesser it's Luma will change)
    public int getInverseMultiplied() {
    	return c().invert( c().multiplied(farb) | (farb&c().masks.A) );
    }
    
    // accent by 180degrees hue shift, calculated 
    // via assuming pythagoras pow(Red,2)*pow(Green,2) 
    // equals pow(Blue,2). for applying 180° degree
    // rotation then each edge's coresponding opposide
    // partials then computed:
    // Red2 will get value of sqrt( 2, pow(Green1,2)/2 + pow(Blue1,2)/2 )
    // Green2 will get value of sqrt( 2, pow(Red1,2)/2 + pow(Blue1,2)/2 )
    // Blue2 will get value of sqrt( 2, pow(Gree1,2)/2 + pow(Red1,2)/2 )
    // Alpha2 will be set directly from Alpha1 being not touched at all
    // (where Luma should not change due it)
    public int getAccentPrecise() {
    	Layout.Index idx = c().index;
    	float r=(int)Math.pow(getChannel(idx.R),2);
    	float g=(int)Math.pow(getChannel(idx.G),2);
    	float b=(int)Math.pow(getChannel(idx.B),2);
    	  int a= getChannel(idx.A);
    	return c().Compose ( 
    		(int)Math.sqrt(g/2+b/2),
    		(int)Math.sqrt(r/2+b/2),
    		(int)Math.sqrt(r/2+g/2), a
    	);
    }
    // fast accent by 'imprecise' 180degrees hue shift
    // much faster then getAccent() but little lossy...
    // (would slowly fade to grey if performed consecutively)
    public int getAccentFast() {
    	Layout.Index idx = c().index;
    	int r=getChannel(idx.R); 
    	int g=getChannel(idx.G);
    	int b=getChannel(idx.B);
    	int a=getChannel(idx.A);
    	return c().Compose (
    		(g+b)/2,(r+b)/2,(r+g)/2,a
    	);
    }
    // check if a pixel can't be 'looked through' (true means alpha is 255)
    public boolean isOpaque() {
    	return getChannel(c().index.A) == 255;
    }
    
    @Override
    public String toString() {
    	Layout.Index idx = c().index;
    	return String.format( "%d %d %d %d",
    		getChannel(idx.R),
    		getChannel(idx.G),
    		getChannel(idx.B),
    		getChannel(idx.A)
    	);
    }
    
  /////////////////////////////////////////////////////////////////////////////////////
  // setters:
    public void setChannel( int index, int value ) {
        farb = ( (farb & ~c().MASKS[index]) | ((value%256)<< c().SHIFT[index]) );
    }
    public Color32 setR(int redValue)
    {
    	farb = ( (farb & ~c().masks.R) | ((redValue%256) << c().shift.R) );
    	return this;
    }
    public Color32 setG(int greenValue)
    {
    	farb = ( (farb & ~c().masks.G) | ((greenValue%256) << c().shift.G) );
    	return this;
    }
    public Color32 setB(int blueValue)
    {
    	farb = ( (farb & ~c().masks.B) | ((blueValue%256) << c().shift.B) );
    	return this;
    }
    public Color32 setA(int alphaValue)
    {
    	setAlpha( alphaValue );
    	return this;
    }
    public void setAlpha( int alpha ) {
        farb = ( farb & ~c().masks.A )|( (alpha%256) << c().shift.A );
    }
    
    
    // sets luma 'imprecise' (lossy) via spliting up third part of given luma change
    // adding it to each color channel. (imprecise and lossy) ... improvment
    // could be changing this to spliting up the luma delta into three 'different' 
    // 'third' parts (wighteninged by the actual R,G,B channel values differences)
    //
    // ... 'different' third parts ... ... reminds me on something like:
    // - mhm, we are two people baked a yummy cake... let's share it between us!
    // everyone of us will get 'half' of it! 
    // - ... in such a case I personally would prefer taking 'the bigger half' then,... 
    // ...ther's no question about it.
    public Color32 setLuma( float luma ) {
    	int delta = (int)(((luma - getLuma()) * 765)/3);
    	int leucht = delta > 0 
    		? Math.min( delta, 255 - Math.max( getR(), Math.max( getG(), getB() ) ) )
    		: Math.max( delta, -Math.min( getR(), Math.min( getG(), getB() ) ) );
    	return setR( getR()+leucht ).setG( getG()+leucht ).setB( getB()+leucht );	
    }

    public void setRGB( int rgb ) {	
    	farb = c().AlphaInsert( rgb, getChannel(c().index.A) );
    }

    public void setRGBA(int rgb, int a) {
    	farb = c().AlphaInsert( rgb, a );
    }

    // assign another color32 value format checked and 
    // gegebenenfalls converted for matching correct order 
    public void assignChecked( Color32 other ) {
    	this.farb = this.c().surface.typecode != other.c().surface.typecode
    			  ? other.c().ConvertInto( this.c(), other.farb )
    			  : other.farb;
    }

    public void assign( Color32 that ) {
    	this.farb = that.farb;
    }

    // assign a color value of matching surface format (without doing format check):
    public void assign( long colorValue ) {
    	farb = (int)colorValue;
    }
    // assign a named color to this instance:
    public Color32 assign( String namedColor ) throws UnknownColor {
    	farb = c().Compose( namedColor, farb );
    	return this;
    }
    
    ///////////////////////////////////////////////////////////////////////////////////////////
    // compositors:
    public Color32 Compose( int r, int g, int b, int a ) { 
    	return createColor( format(), r, g, b, a );
    }
    public Color32 Compose( int r, int g, int b ) {
    	return clone().setR( r ).setG( g ).setB( b );
    }
    public Color32 Compose( int rgb, int a ) {
        return createColor( format(), rgb, a );
    }
    public Color32 Compose( String namedColor ) throws UnknownColor {
    	Color32 copy = this.clone();
    	copy.farb = c().Compose( namedColor, farb );
    	return copy;
    }
    
    ///////////////////////////////////////////////////////////////////////////////
    // interobjective mixing:
    
    // here 'that' pixel (where we will lay over) is treated being opaque!
    // (even if it is NOT opaque indeed). alpha value will be discarded.
    // for mixing into layers containing alpha, use 'layOverRGBA()' instead 
    public int layOverRGB( Color32 that ) {
    	if(c().surface.typecode == that.c().surface.typecode)
    		return this.c().multiplied(this.farb)
    		     + that.c().multiplied(that.farb,255);
    	else return c().multiplied(c().ConvertFrom(that.c(),that.farb),255)
    			  + this.c().multiplied(this.farb);
    }
    
    // lay this Color32 value over that opaque 'rgb' value 
    public int layOverRGB( int alphaRemoved ) {
    	return c().multiplied( c().AlphaInsert(alphaRemoved,255), 255-getAlpha() )
    		 + c().multiplied( farb );
    }
    
    // lay this color (but with 'alpha' exchanged) over opaque 'rgb'
    // here 'rgb' parameter is supposed to come in 'R->G->B' ordered!
    // and NOT to come in by correct, matching surface format order
    public int layOverRGB(int rgb, int alpha) {
    	int stored = getAlpha();
    	rgb = setA( alpha ).layOverRGB( rgb );
    	setAlpha( stored );
    	return rgb;
    }

    // get a 4channel pixel value which is result of
    // laying 'this' 4channel-pixel
    //   over 'that' 4channel-pixel 
    // (does format conversion when different formats) 
    public int layOverCheckedRGBA( Color32 that ) {
    	Color32Surface.Layout tis = this.c();
    	Color32Surface.Layout tas = that.c();
    	int thisA = this.getAlpha();
    	int thatA = that.getAlpha();
        return (thisA > thatA
             ? (thisA << tas.shift.A)
             : (thatA << tas.shift.A))
             | ( tas.ConvertFrom(tis,tis.multiplied(farb))
               + tas.multiplied(that.farb,255-thisA) );
    }

    public int layOverRGBA( Color32 that ) {
    	Color32Surface.Layout s = c();
    	int thisA = this.getAlpha();
    	int thatA = that.getAlpha();
        return ( (thisA > thatA) 
        	 ? (thisA << s.shift.A)
        	 : (thatA << s.shift.A) )
        	 | (s.multiplied( farb )
        	  + s.multiplied( that.farb, 255-thisA ) );
    }

    // helper for merging layers by 'mergeLayers(zAxis)' function
    protected boolean mergeWith(Color32 other) {
    	boolean GroundHit = other.isOpaque();
        other.farb = GroundHit 
        		   ? this.layOverRGB(other.farb) 
        		   : this.layOverRGBA(other);
        this.farb = 0;
        return GroundHit;
    }
    
    // merge pixels along axis or ray through several layers
    // (takes array of pixels laying on a merge axis or ray)
    public Color32 mergeLayers(Color32[] Zaxis)
    {   
    	Color32 next = this;
    	int layer = Zaxis.length-1;
    	while( ( layer = next.mergeWith(next=Zaxis[layer--]) 
    			       ? 0 : layer ) > 0 );
    	return next;
    }
}

