package postprocessor.color;

import postprocessor.struct.Struct;

public class Image32 {
	public int X, Y, W, H;
	public @Struct.Point long Size() {return Struct.newPoint(W,H);}
	public @Struct.Point long Position() {return Struct.newPoint(X,Y);}
	public Layer32 layer;
	public Image32( int w, int h, Color32Surface.FORMAT format ) {
		X=0; Y=0; W=w; H=h;
		layer = new Layer32( Size(), format );
		layer.setImage( this );
	}
	public Image32( Layer32 asImage ) {
		X = asImage.OffsetX();
		Y = asImage.OffsetY();
		W = asImage.Width();
		H = asImage.Height();
		asImage.setImage( this );
	}
	public Color32 getPixel(int x, int y) {
		return layer.getPixel(x+X,y+Y);
	}
	public void addLayer() {
		layer.newLayer( Size(), Position() );
	}
	public void addLayer( Layer32 newlayer ) {
		layer.addLayer( newlayer, Position() );
	}
	public void setData( Layer32 setImageLayer ) {
		setImageLayer.setImage( this );
	}
	public Color32Surface.FORMAT format() {
		return layer.format();
	}
}
