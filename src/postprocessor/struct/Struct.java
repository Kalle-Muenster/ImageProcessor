package postprocessor.struct;
import java.lang.annotation.*;

import postprocessor.color.Color32;
import postprocessor.color.Color32Surface;

public class Struct
{
	@Target({ElementType.METHOD,ElementType.TYPE,ElementType.PARAMETER,ElementType.FIELD,ElementType.LOCAL_VARIABLE})
	public @interface Point {   
		public long data() default 0;
		public int x() default 0;
		public int y() default 0;
	}  
	
	public static @Point long newPoint( int x, int y )
	{
		return ((long)x & 0x00000000FFFFFFFFl)|((long)y<<32);
	}

	public static @Point long setX(@Point long point, int x)
	{
		return ( point  & 0xFFFFFFFF00000000l) 
			 | ((long)x & 0x00000000FFFFFFFFl);	
	}
	
	public static @Point long setY(@Point long point, int y)
	{
		return ( point & 0x00000000FFFFFFFFl )
			 | ((long) y << 32);	
	}
	
	public static int getX(@Point long point)
	{
		return (int)( point & 0x00000000FFFFFFFFl );
	}
	
	public static int getY(@Point long point)
	{
		return (int)( ( point & 0xFFFFFFFF00000000l ) >> 32 );
	}
	public static int indices( @Point long areal ) {
		return (int)(getX(areal) * getY(areal));
	}
	
	// subtract point 'from' from point 'to'
	public static @Point long vector( @Point long from, @Point long to )
	{
		return newPoint( getX(to)-getX(from), getY(to)-getY(from) );
	}

	// add point 'from' to point 'to' 
	public static @Point long offset( @Point long from, @Point long to )
	{
		return newPoint( getX(to)+getX(from), getY(to)+getY(from) );
	}

	public static float distance( @Point long from, @Point long to )
	{
		to = vector( from, to );
		return (float)Math.sqrt( Math.pow(getX(to),2) + Math.pow(getY(to),2) ); 
	}

	public static @Point long scaled( @Point long vec, float scalar ) {
		return newPoint((int)(getX(vec)*scalar),(int)(getY(vec)*scalar));
	}

	public static int indexWithin( @Point long field, @Point long point )
	{
		return getX(field) * getY(point) + getX(point);
	}
	
	public static @Point long pointWithin( @Point long field, int index )
	{
		int width = getX(field);
		return newPoint( index % width, index / width );
	}
	
	public static int indexWithin( @Point long field, @Point long oset2D, @Point long point )
	{
		return ( getX(field) * (getY(point) - getY(oset2D)) )
			 + ( getX(point) - getX(oset2D) );
	}
	
	public static @Point long pointWithin( @Point long field, @Point long oset2D, int index )
	{
		int width = getX(field);
		return newPoint( (index % width) - getX(oset2D), (index / width) - getY(oset2D) );
	}
	
	public static int indexWithin( @Point long field, int oset1D, @Point long point ) {
		return indexWithin( field, indexWithin( field, oset1D ), point );
	}
}


