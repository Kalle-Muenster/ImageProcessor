package postprocessor;

import java.text.ParseException;

import postprocessor.ImageProcessor.MarkerParameter;
import postprocessor.ImageProcessor.DIRECTION;
import postprocessor.ImageProcessor.SHAPE;
import postprocessor.color.Color32;
import postprocessor.struct.*;


public class PostProcessor
{
    private static ImageProcessor proc = null;
    private static boolean quit = false;
    private static boolean no_rename = false;
    private static boolean output_name = false;
    private static java.text.NumberFormat numberParser = java.text.NumberFormat.getInstance();
    
    private static int parseShapeArgument( MarkerParameter state, String arg ) 
    {
		switch (arg) {
		case "CIRCLE":
			state.Shape = SHAPE.CIRCLE;
			break;
		case "ELLYPS":
			state.Shape = SHAPE.ELLYPS;
			break;
		case "SQUARE":
			state.Shape = SHAPE.SQUARE;
			break;
		case "ARROW":
			state.Shape = SHAPE.ARROW;
			break;
		case "CROSS":
		case "CROSSHAIR":
			state.Shape = SHAPE.CROSS;
			break;
		default:
			state.Shape = SHAPE.UNKNOWN;
			return 0;
		} return 1;
    }
    
    private static int parsePositionArguments( MarkerParameter state,
    		                                   int i, String[] args ) 
    {
    	if (i + 2 < args.length) {
        	numberParser.setParseIntegerOnly(true);
    		boolean positionSet = false;
    		try { // try to collect and set parameters:
    			state.Areal.x = (int) numberParser.parse(args[i + 1]).shortValue(); i++;
    			state.Areal.y = (int) numberParser.parse(args[i + 1]).shortValue(); i++;
    			state.Areal.width = state.Areal.height = -1;
    			positionSet = true;
    			if (i + 1 < args.length) {
    				state.Areal.width = (int) numberParser.parse(args[i + 1]).shortValue();	i++;
    				state.Areal.height = state.Areal.width;
    			}
    			if (i + 1 < args.length) {
    				state.Areal.height = (int) numberParser.parse(args[i + 1]).shortValue(); i++;
    				if (state.Shape == SHAPE.UNKNOWN) {
    					state.Shape  = SHAPE.SQUARE;
    				}
    			}
    		} catch (Exception e) {
    			if (!positionSet)
    				return -i;
    		} return i;
        } return -(i+2);
    }

    public static int processCommandLineArgs(String[] args)
    {// create an instance by giving path to image file.
    	int offset = no_rename ? 1 : 0;
    	try { proc = new ImageProcessor( args[offset++] );
    		if (output_name) { ++offset;
    			proc.setImagePath( args[offset++] );
    			no_rename = true;
    		}
    	} catch(Exception FileIOError) {
    		proc=null;
    		System.err.print("Error loading File: "+args[no_rename?1:0]);
    		return 1;
    	} MarkerParameter state = proc.copyCurrentState();
    	boolean drawOutline = false;
	 // parse arguments and call postprocessor functions on known commands
		for (int i = offset; i < args.length; i++) {
			int[] parameter = new int[4];

			switch (args[i])
			{
			case "flipImage":
				if (i + 1 < args.length) {
					switch(args[i+1])
					{
					case "UP":
						proc.flipImage(DIRECTION.UP);
						break;
					case "RIGHT":
						proc.flipImage(DIRECTION.RIGHT);
						break;
					default:
						i++;
						System.err.printf("parameter error [#%s:%s]: valid flipImage parameters are: 'UP' or 'RIGHT'",i,args[i]);
						return i;
					} ++i;
				} break;
			case "cuttenEdges":
				if (i + 1 < args.length) {
					numberParser.setParseIntegerOnly(true);
					try { // try if parameters where given. if not, use default settings
						parameter[0] = numberParser.parse(args[i + 1]).intValue(); i++;
						if (i + 3 < args.length) {
							parameter[1] = numberParser.parse(args[i + 1]).intValue(); i++;
							parameter[2] = numberParser.parse(args[i + 1]).intValue(); i++;
							parameter[3] = numberParser.parse(args[i + 1]).intValue(); i++;
							proc.cuttenEdges( parameter[0],
											  parameter[1],
											  parameter[2],
											  parameter[3] );
							break;
						} else {
							proc.cuttenEdges(parameter[0]);
							break;
						}
					} catch (Exception e) {
					 	// if there where no parameters passed, do nothing and continue
					  // by using the parameter-less cuttenEdges overload.
					}
				} proc.cuttenEdges();
				break;
			case "roundCorners":
				float sizeA = 0, sizeB = 0;
				if (i + 1 < args.length) {
					numberParser.setParseIntegerOnly(false);
					try { // try if parameters where given.
						sizeA = sizeB = (float) numberParser.parse(args[i + 1]).doubleValue(); i++;
						if (i + 1 < args.length) { sizeB = 0;
							sizeB = (float) numberParser.parse(args[i + 1]).doubleValue(); i++;
						}
					} catch (Exception e) {
						// if there where no parameters: do nothing and continue
					  // with roundCorners() passing 0.0, 0.0 as default parameter
					}
				} proc.roundCorners(sizeA,sizeB);
				break;
			case "drawMarker":
				state.assign( proc.CurrentMarker );
				if (i + 1 < args.length) {
					i += parseShapeArgument( state, args[i+1] );
				}
					if( (i=parsePositionArguments( state, i, args )) < 0 ) {
						System.err.printf("wrong positioning parameter [#%s:%s] for drawMarker\n",i,args[i]);
						return -i;
					}
					proc.CurrentMarker.assign( state );
					proc.drawMarker();
				break;
			case "showObject": 
				state.assign( proc.CurrentMarker );
				if (i + 1 < args.length) 
					i += parseShapeArgument( state, args[i+1] );
				int numberOfArgs = parsePositionArguments(state,i,args) - i;
				if( numberOfArgs == 2 ) { 
					proc.showObject( state.Areal.x, state.Areal.y );
				} else if( numberOfArgs > 2 ) {
					proc.CurrentMarker.assign( state );
					proc.showObject( state.Shape );
				} else { // if numberOfArgs < 2
					System.err.printf("wrong positioning parameter [#%s:%s] for showObject.\n",i,args[i]);
					return -(numberOfArgs+i);
				} i+=numberOfArgs;
				break;
			case "dragObject":
				if (i + 1 < args.length) 
					i += parseShapeArgument( state, args[i+1] );
				int countArgs = parsePositionArguments( state, i, args ) - i;
				if( countArgs == 4 ) {
					proc.CurrentMarker.Shape = state.Shape;
					proc.dragObject( Struct.newPoint(state.Areal.x,state.Areal.y),
							         Struct.newPoint(state.Areal.width,state.Areal.height) );
				} else {
					System.err.printf("parameter [#%s:%s] - dragObject takes at least 4 parameter.\n",i,args[i]);
					return -(countArgs+i);
				} i += countArgs;
				break;
			case "inlineImage": 
				if( i + 1 < args.length ) {
					try { short stroke = numberParser.parse(args[i+1]).shortValue(); ++i;
						proc.inlineImage( (float)stroke );
					} catch(Exception ex) { ++i;
						System.err.printf( "Invalid parameter [#%s:%s] for drawing inline!\n",
				                                            i, args[i] ); 
						return i;
					}
				} break;
			case "outlineImage": 
				if( i + 1 < args.length ) {
					try { short stroke = numberParser.parse(args[i+1]).shortValue(); ++i;
						proc.outlineImage( (float)stroke );
					} catch(Exception ex) { ++i;
						System.err.printf( "Invalid parameter [#%s:%s] for drawing outline!\n",
				                                            i, args[i] ); 
						return i;
					}
				} break;
			case "setColor":
				if (i + 1 < args.length) {
					Color32 color = proc.CurrentMarker.Color.clone();
					boolean colorWasSet = false;
					if( args[i+1].startsWith("0x") ) {// at first check if maybe a hex value was passed:
						try { color.assign( args[++i] ); colorWasSet=true;
						} catch( Color32.UnknownColor ex ) { i++;
							System.err.printf( "Hex color value [#%s:%s] must have 6 or 8 digits!\n",
						                       i, args[i] ); 
							return i;
						}
					} int MarkerColor = color.getRGB();
					short MarkerAlpha = (short)color.getAlpha();
					if (!colorWasSet) { numberParser.setParseIntegerOnly(true);
						try {// collect a color value:
							MarkerColor = (int) numberParser.parse(args[i + 1]).longValue(); ++i;
							colorWasSet = true;
							if ( i+2 < args.length ) {
								color.setChannel(color.layout().index.R,(short)MarkerColor);
								color.setChannel(color.layout().index.G,numberParser.parse(args[i+1]).shortValue()); ++i;
								color.setChannel(color.layout().index.B,numberParser.parse(args[i+1]).shortValue()); ++i;
								MarkerColor = color.getRGB();
								if ( i+1 < args.length ) {
									color.setAlpha(numberParser.parse(args[i+1]).shortValue()); ++i;
									MarkerAlpha = (short)color.getAlpha();
							    }
							} 
						} catch ( ParseException e ) {
							if (!colorWasSet) { 
								try { color.assign( args[i+1] ); i++;								
								} catch ( Color32.UnknownColor colorNameError ) {
									System.err.println( colorNameError.getMessage() );
								    System.err.printf( "Invalid parameter [#%s:%s] for setting color!\n",
									                   i+1, args[i+1] ); 
								    return i+1;
								}
							} else {
								color.setRGBA( MarkerColor, MarkerAlpha );
							}
						} 								
					}  proc.setMarkerColor( color );
				} 
				break;
			case "rotateImage":
				if (i + 1 < args.length) {
					numberParser.setParseIntegerOnly(true);
					try { parameter[0] = numberParser.parse(args[i+1]).intValue(); i++;
					} catch(ParseException notANumber) { parameter[0] = 3; }
				} proc.counterClock( parameter[0] );
				break;
			case "cropFrame":
				if (i + 4 < args.length) {
					numberParser.setParseIntegerOnly(true);
					try { // try if all parameters are given. 
						parameter[0] = numberParser.parse(args[i + 1]).intValue(); i++;
						parameter[1] = numberParser.parse(args[i + 1]).intValue(); i++;
						parameter[2] = numberParser.parse(args[i + 1]).intValue(); i++;
						parameter[3] = numberParser.parse(args[i + 1]).intValue(); i++;
						proc.cropFrame( parameter[0],
										parameter[1],
										parameter[2],
										parameter[3] );
						break;
					} catch (ParseException e) { i++; //...if not, cancel operation.
						System.err.printf("invalide parameter [%s:%s] for cropFrame!\n",i,args[i]);
						return i;
					}
				}
				break;
			case "drawLine":
				if(i+4<args.length) {
					numberParser.setParseIntegerOnly(true);
					try {
						parameter[0] = numberParser.parse(args[i + 1]).intValue(); i++;
						parameter[1] = numberParser.parse(args[i + 1]).intValue(); i++;
						parameter[2] = numberParser.parse(args[i + 1]).intValue(); i++;
						parameter[3] = numberParser.parse(args[i + 1]).intValue(); i++;
					} catch (ParseException e) { i++;
						ImageProcessor.displayUsage();
						System.err.printf( "Invalid parameter [#%s:%s] for drawiLine\n",
								           i,args[i]); return i;
					}
				} if(i+1<args.length) {
					state.Lines = proc.CurrentMarker.Lines;
					numberParser.setParseIntegerOnly(false);
					try { state.Lines = numberParser.parse(args[i + 1]).floatValue(); i++;
					} catch(Exception e) {
						state.Lines = proc.CurrentMarker.Lines;
					} proc.drawLine( Struct.newPoint(parameter[0], parameter[1]),
					                 Struct.newPoint(parameter[2], parameter[3]),
					                 state.Lines );
				} else proc.drawLine( parameter[0], parameter[1],
		                   			  parameter[2], parameter[3] );
				break;
			default:
				ImageProcessor.displayUsage();
				return i;
			}// close switch

		}// close parser

	  // store processed image to disk.
		proc.saveImageFile(no_rename);
        return 0;
    }

    public static void main(String[] args)
    {// check for "--help" and "--no-rename" arguments...

        for (String arg : args) {
            if (arg.equals("--help")) {
                ImageProcessor.displayUsage();
                System.exit(0);
            }
            if (!(no_rename || output_name)) {
            	if (arg.equals("--no-rename")) {
            		no_rename = true;
            	} else if (arg.equals("--out-image")) {
            		output_name = true;
            	}
            }
        }

       // check the command-line if parameters
      // where passed and execute:
        if (args.length > 1)
        	System.exit( processCommandLineArgs(args) );
        else
        	ImageProcessor.displayUsage();

        System.exit(0);
    }

}
