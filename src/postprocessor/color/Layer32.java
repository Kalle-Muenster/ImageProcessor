package postprocessor.color;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.util.LinkedList;
import java.util.List;
import postprocessor.color.Color32;
import postprocessor.color.Color32Surface;
import postprocessor.color.Color32.UnknownColor;
import postprocessor.color.Color32Surface.FORMAT;
import postprocessor.color.Color32Surface.Layout;
import postprocessor.struct.Struct;

import org.eclipse.swt.graphics.Rectangle;




public class Layer32< SurFace extends Color32Surface.Layout > extends Color32 
{
	public enum NonLocated {
		BeforeBegin, AfterTheEnd, RoundCorner, NoDirection,
	}
	public static class Directive {
		public final int        direct;
		public final NonLocated origin;
		public Directive( NonLocated originating ) {
			origin = originating;
			switch ( origin ) {
				default:
				case NoDirection: direct =  0; break;
				case BeforeBegin: direct =  1; break;
				case AfterTheEnd: direct = -1; break;
				case RoundCorner: direct =  3; break;
			}
		}
	}

	public static Directive Forwards = new Directive( NonLocated.BeforeBegin );
	public static Directive Backward = new Directive( NonLocated.AfterTheEnd );
	public static Directive Diagonal = new Directive( NonLocated.RoundCorner );
	public static Directive Disabled = new Directive( NonLocated.NoDirection );

	
	private Color32Surface.Layout layout;
	private Image32               view;
	private int[]                 data;
	private @Struct.Point long    size;
	private @Struct.Point long    offset;
	private int                   position;
	private Layer32               below;
	private Layer32               above;
	private Color32               color;
	private int                   flags;
	
	protected Layer32( @Struct.Point long ImgSize, FORMAT ImgFormat, int[] ImgData ) {
		super();
		switch( ImgFormat ) {
			case RGBA: layout = new Color32Surface.RGBA(); break;
			case ARGB: layout = new Color32Surface.ARGB(); break;
			case BGRA: layout = new Color32Surface.BGRA(); break;
			case ABGR: layout = new Color32Surface.ABGR(); break;
			case GRAY: layout = new Color32Surface.GRAY(); break;
		} 
		size  = ImgSize;
		data  = ImgData;
		offset = 0;
		position = 0;
		farb  = data[position];
		above = null;
		below = null;
		color = createColor(ImgFormat);
		flags = 0;
		view = null;
	}

	protected Layer32( Layer32 on, @Struct.Point long wh, @Struct.Point long at )
	{
		this( wh, on.format(), new int[ Struct.indices(wh) ] );
		this.color = on.color == null
				   ? on.color = createColor( on.format() )
				   : on.color;
		this.below = on;
		if ( wh == on.size ) {
			this.offset = 0;
			this.position = Struct.indexWithin(wh,at);
		} else {
			this.position = 0;
			this.offset = at;
		} this.flags = 0;
		this.view = on.view;
	}

	protected Layer32( Layer32 on )
	{
		this( on, on.Size(), on.Point() );
		this.below = on;
		this.color = on.color == null
				   ? on.color = createColor( on.format() )
				   : on.color;
		this.offset = on.offset;
        this.position = Struct.indexWithin(size,offset);
        this.flags = 0;
        this.view = on.view;
	}

	public Layer32( @Struct.Point long imgsize, FORMAT imgsurf )
	{
		super();
		switch( imgsurf ) {
			case RGBA: layout = new Color32Surface.RGBA(); break;
			case ARGB: layout = new Color32Surface.ARGB(); break;
			case BGRA: layout = new Color32Surface.BGRA(); break;
			case ABGR: layout = new Color32Surface.ABGR(); break;
			case GRAY: layout = new Color32Surface.GRAY(); break;
		} 
		data = new int[Struct.indices(imgsize)];
		size = imgsize;
		flags = 0;
		farb = data[position=0];
		above = null;
		below = null;
		color = createColor( imgsurf );
		view = null;
	}
	
	public static Layer32 createFromData( int[] imgData, @Struct.Point long imgSize, Color32Surface.FORMAT imgFormat ) {
		Layer32 newlayer = new Layer32( imgSize, imgFormat, imgData );
		newlayer.farb = imgData[0];
		newlayer.position = 0;
		return newlayer;
	}
	
	public static Layer32 createLayer( FORMAT fmt ) {
		Layer32 n = new Layer32( Struct.newPoint( 1, 1 ), fmt );
		n.farb = 0;
		n.position = 0;
		n.apply();
		return n;
	}
	
	public static Layer32 createLayer( int width, int height, FORMAT fmt ) {
		Layer32 n = new Layer32( Struct.newPoint( width, height ), fmt );
		n.farb = 0;
		n.position = 0;
		n.apply();
		return n;
	}

	@Override
	protected Color32Surface.Layout c() {
		return layout;
	}

	public void setImage( Image32 attachedTo ) {
		attachedTo.layer = getBottom();
		Layer32 next = attachedTo.layer;
		while( next != null ) {
			next.view = attachedTo;
			next = next.above;
		}
	}
	
	public void setData(int[] newData) throws ArrayIndexOutOfBoundsException {
		if ( newData.length < data.length )
			throw new ArrayIndexOutOfBoundsException("layer bounds excceed new data size");
		data = newData;
		enter( position );
	}
	
	public int[] swapData(int[] newData) throws ArrayIndexOutOfBoundsException {
		int[] oldData = data;
		setData( newData );
		return oldData;
	}
	
	public int[] getData() {
		leave();
		return data;
	}

	public void addLayer( Layer32 newlayer, @Struct.Point long xyoffset ) {
		Layer32 toplayer = getTopmost();
		toplayer.above = newlayer;
		newlayer.below = toplayer;
		newlayer.offset = xyoffset;
		newlayer.view = this.view;
	}
	
	// clones a new Layer32 instance from this layer as exact copy of whole data
	// (but returns it as as Color32 object, which needs up cast before can use)  
	@Override
	public Color32 clone() {
		Layer32 create = new Layer32( this.size, c().surface.getFormat() );
		create.assign( this );
		return create;
	}
	
	// positioning and sizing the layer  ///////////////////////////////////////
	public int Width() {
		return Struct.getX( size );
	}
	public int Height() {
		return Struct.getY( size );
	}
	public @Struct.Point long Size() {
		return size;
	}
	public void Size( int x, int y ) {
		size = Struct.newPoint( x, y );
	}
	public void Size( @Struct.Point long newSize ) {
		size = newSize;
	}

	public Rectangle Rect() {
		return new Rectangle( OffsetX(), OffsetY(), Width(), Height() );
	}
	
	
	// get top/left corner (corner '0') of this Layer32 
	public @Struct.Point long getCorner() {
		return getCorner(0);
	}

	// corner 0: top/left
	// corner 1: top/right
	// corner 2: bottom/right
	// corner 3: bottom/left
	public @Struct.Point long getCorner(int number) throws ArrayIndexOutOfBoundsException {
		switch(number) {
		case 0: return offset;
		case 1: return Struct.newPoint( OffsetX()+Width(), OffsetY() );
		case 2: return Struct.offset(offset,size);
		case 3: return Struct.newPoint( OffsetX(), OffsetY()+Height() );
		default: throw new ArrayIndexOutOfBoundsException("ein viereck hatt vier ecken");
		}
	}
	public @Struct.Point long getCenter() {
		return Struct.offset( offset, Struct.offset( Struct.scaled(size,0.5f), Struct.newPoint(1,1) ) );
	}
	public Color32 getCenterPixel() {
		farb = data[ position = Struct.indexWithin( size, Struct.offset( Struct.scaled( size, 0.5f ), Struct.newPoint(1,1) ) ) ];
		return this;
	}
	
	public @Struct.Point long ViewSize() {
		return view == null ? getBottom().Size() : view.Size();
	}
	
	public @Struct.Point long ViewPoint() {
		return view == null ? offset : Struct.vector( offset, view.Position() );
	}
	public Layer32 newLayer()	{
		Layer32 topmost = getTopmost();
		topmost.above = new Layer32( topmost );
		if (view != null) topmost.above.setImage( view );
		return topmost.above;
	}
	
	public Layer32 newLayer( @Struct.Point long ofSize, @Struct.Point long atPoint ) {
		Layer32 topmost = getTopmost();
		topmost.above = new Layer32( topmost, ofSize, atPoint );
		if (view != null) topmost.above.setImage( view );
		return topmost.above;
	}
	
    public Layer32 assign( Layer32 that )
    {
    	int count = that.Width() * that.Height();
    	if ( c().surface == that.c().surface ) {
    		if ( this.data.length < count ) {		
    			data = that.data.clone();
    		} else for( int i=0; i < count; ++i ) {
				data[i] = that.data[i];
			} this.size = that.size;
    	} else {
    		if ( this.data.length < count ) {
    			data = new int[count];
    		} this.size = that.size;
    		int pos = that.position;
    		this.letsGo( Forwards );
    		that.letsGo( Forwards );
    		for ( int i=0; i<count; ++i ) {
    			this.setNext( that.getNext().farb );
    		} that.position = pos;
    	}
    	position = that.position;
    	flags = that.flags;
    	farb = that.farb;
    	return this;
    }

    // Navigation within data  //////////////////////
    
    private int leave() {
    	if ( position >= 0 && position < data.length ) {
			data[ position ] = farb;
		} return position;
    }
    private int leave( int frompos ) {
    	if ( frompos >= 0 && frompos < data.length ) {
			data[ frompos ] = farb;
		} return frompos;
    }
    private int enter() {
    	if ( position >= 0 && position < data.length ) {
			farb = data[ position ];
		} else { farb = 0; }
    	return farb;
    }
    private int enter( int newpos ) {
    	if ( newpos >= 0 && newpos < data.length ) {
			farb = data[ position = newpos ];
		} else { farb = 0; }
    	return farb;
    }

    private int pixel( int newpos ) {
    	if ( newpos >= 0 && newpos < data.length ) {
			return data[ position = newpos ];
		} else { return 0; }
    }
    
	private boolean moveTo( int nextpos )	{
		if( leave() != nextpos )
			enter( nextpos );
		return position == nextpos;
	}
	
	public void apply() {
		data[position] = farb;
	}
	
	// Acting as Z-Axis Iterator ////////////////////
	
	public Layer32 nextBelow()
	{
		if( below != null ) { // Y looks like rocket flying downwards - so 'Y' will be the name for offset value in relation to the lower layers below this one  
			below.position = this.position + Struct.indexWithin(size,this.offset);
			return below;
		} return this;
	}
	public Layer32 nextAbove()
	{
		if( above != null ) { // X looks like having arms wide open upward! so 'X' will be the name for offset value in relation to the upper layers above this one
			above.position = this.position - Struct.indexWithin(size, above.offset);
			return above;
		} return this;
	}

	// first pixel a ray cast along Z-Axis 'would' hit
	public Layer32 getTopmost()
	{
		Layer32 topmost = this.nextAbove();
		while( topmost.above != null ) {
			topmost = topmost.nextAbove();
		} return topmost; 
	}

	// last pixel a ray cast along Z-Axis 'could' hit  
	public Layer32 getBottom()
	{
		Layer32 bottom = this.nextBelow();
		while( bottom.below != null )
			bottom = bottom.nextBelow();
		return bottom;
	}
	
	// get an array of pixels laying on a z-ray starting at this layers z-index at the given x, y position
	// directed downward... the array will contain copies of the pixels (so no references to layer pixels)  
	public Color32[] ray( int x, int y )
	{
		List<Color32> axlist = new LinkedList<Color32>();
		Layer32 nexte = setPosition(x,y);
		axlist.add( nexte.getPixel() );
		int count = 1;
		while( nexte.below != null ) { ++count;
			axlist.add( (nexte = nexte.nextBelow()).getPixel() );
		} return axlist.toArray( new Color32[count] );
	}
	
	// on the bottom, down below this layers given X/Y position, an iteration along z-axis will be
	// started (as a wave coming from below actual x,y position, floating upward towards all layer)  
	public Color32 fromBelow(int x, int y)
	{
		Layer32 bottom = this;
		while( bottom.below != null && !bottom.isOpaque() ) bottom = bottom.below;
		color.farb = bottom.setPosition(x,y).getPixel().getValue();
		return ( bottom.above != null ) ? bottom.nextAbove().fromBelow() : color;
	}
	
	// on the topmost layer, above this layers given X/Y position, an iteration along z-axis will be
	// started (as a wave/ray seeking from above x,y position, drilling downward towards all layers) 
	public Color32 fromAbove( int x, int y )
	{
		Layer32 topmost = this;
		while( topmost.above != null ) topmost = topmost.above;
		color.farb = topmost.setPosition(x,y).getPixel().getValue();
		return ( !color.isOpaque() && topmost.below != null )
			 ? topmost.nextBelow().fromAbove() : color;
	}
	
	// (private) internally called fromAbove(x,y) for invoking recursive iteration which
	// retrieves 'merged' color value when merging or flattening layers 'fromAbove'  
	private Color32 fromAbove() 
	{
		color.farb = color.layOverRGBA( getPixel() );
		return ( isOpaque() || below == null ) 
			 ? color : nextBelow().fromAbove();
	}

	// (private) internally called fromBelow(x,y) for invoking recursive iteration which
	// retrieves 'merged' color value when flattening or merging layers 'fromBelow' 
	private Color32 fromBelow()
	{
		color.farb = getPixel().layOverRGBA( color );
		return ( above != null ) ? nextAbove().fromBelow() : color; 
	}
	

	// Acting as (one dimensional) surface Iterator ///////////////////////
	
	// prepare for starting an iteration!
	public Layer32 letsGo( Directive state ) {
		int lastpos = leave();
		flags = state.direct;
		switch( state.origin ) {
		case BeforeBegin: position = -1;  break;
		case AfterTheEnd: position = data.length; break;
		case RoundCorner: position = -Width();
		default: position = lastpos;
		} return this;
	}
	
	public @Struct.Point long Point() {
		return Struct.pointWithin( size, position );
	}
	public int PointX() {
		return Struct.getX( Struct.pointWithin( size, position ) );
	}
	public int PointY() {
		return Struct.getY( Struct.pointWithin( size, position ) );
	}
	public int Index() {
		return position;
	}
	
	public @Struct.Point long globalPoint() {
		return Struct.pointWithin(
		    getBottom().Size(), offset, position
		                           );
	}
	
	public int globalIndex() {
		return Struct.indexWithin(
		    getBottom().Size(), offset, position
		                           );
	}
		
	public boolean nextStep()
	{
		if ( !moveTo( position + flags ) ) {
			flags = Disabled.direct;
			return false;
		} return true;
	}
	
	public boolean backStep()
	{
		if ( !moveTo( position - flags ) ) {
			flags = Disabled.direct;
			return false;
		} return true;
	}
	
	public boolean setNext( Color32 pixel )
	{
		if ( nextStep() ) { 
			setPixel( pixel );
			return true;
		} else return false;
	}
	
	public boolean setNext( int pixel )
	{
		if ( nextStep() ) { 
			setPixel( pixel );
			return true;
		} else return false;
	}
	
	public Layer32 getNext()
	{
		if ( !nextStep() ) {
			if ( flags != Disabled.direct )
			    if ( !moveTo( flags == Forwards.direct ? 0 : data.length - 1 ) ) {
			    	flags = Disabled.direct; return null;
			    }
		} return this;
	}
	
	
	// positioning access point to a pixel  /////////////////////////////////////////////////////////
	
	public Layer32 directX( int pixels ) {	
		long ixypspos = Struct.pointWithin( size, position );
		position = Struct.indexWithin( size, Struct.setX( ixypspos, Struct.getX(ixypspos) + pixels ) );
		return this;
	}
	public Layer32 directYps( int pixels )	{	
		long ixypspos = Struct.pointWithin( size, position );
		position = Struct.indexWithin( size, Struct.setY( ixypspos, Struct.getY(ixypspos) + pixels ) );
		return this;
	}
	public Layer32 direct( @Struct.Point long vector ) {
		position = Struct.indexWithin( size, Struct.offset( vector, Struct.pointWithin( size, position ) ) );
		return this;
	}
	
	public Layer32 setPosition( int index ) {
		position = index;
		return this;
	}
	public Layer32 setPosition( int x, int y ) {
		position = Struct.indexWithin( this.size, this.offset, Struct.newPoint(x,y) );
		return this;
	}
	
	public void setPixel( int color ) {
		data[position] = farb = color;
	}
	public void setPixel( Color32 color ) {
		assignChecked( color );
		data[position] = farb;
	}
	public void setPixel( int rgb, int alf ) {
		data[position] = farb = c().AlphaInsert(rgb,alf);
	}
	public void setPixel( short r, short g, short b, short a ) {
		data[position] = farb = c().Compose( r, g, b, a );
	}
	public void setPixel( @Struct.Point long atPoint, int toColor ) {
		data[ Struct.indexWithin( size, Struct.offset(atPoint,offset) ) ] = toColor;
	}
	public void setPixel( @Struct.Point long atPoint, Color32 toColor )	{
		farb = toColor.getValue();
		position = leave( Struct.indexWithin( size, Struct.offset(atPoint,offset) ) );
	}
	public void setPixelChecked( int x, int y, Color32 color ) {
		int newpos = Struct.indexWithin( size, Struct.offset(Struct.newPoint(x,y),offset) );
		farb = enter( newpos );
		assignChecked( color );
		if ( position == newpos )
			data[ position ] = farb;
	}
	
	public Color32 getPixel() {
		farb = pixel( position );
		return this;
	}
	public Color32 getPixel( int x, int y ) {
		farb = pixel( Struct.indexWithin( size, Struct.offset( offset, Struct.newPoint(x,y) ) ) );
		return this;
	}
	@Override
	public int getValue() {
		return farb = pixel( position );
	}
	public int getValue( int x, int y ) {
		return farb = pixel( Struct.indexWithin( size, Struct.offset( offset, Struct.newPoint(x,y) ) ) );
	}

	
	
	// positioning a whole layer (in relation to other layers above or below) //////////
	
	public void moveLayer(int x, int y) {
		offset = Struct.newPoint(x,y);
	}
	
	public int OffsetX() { // x offset to the lower layers x dimension
		return Struct.getX(offset);
	}
	
	public int OffsetY() { // y offset to the lower layers y dimension
		return Struct.getY(offset);
	}
	
	public int IndexOffset() { // i offset to the lower layers data array dimension
		return Struct.indexWithin( getBottom().Size(), offset );
	}



}
