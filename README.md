# ImageProcessor
Command line tool (written in Java) for doing simple postproccessing operations on screenshots


A small java commandline tool for performing simple post processing operations on screenshot like drawing markers, arrows, croping regions and so on. It's quite handy to be used within automated proccessing of documentation for software where often screenshots pointing out operations on distinct buttons, menues or othewr items by drawn marker overlays like rectangles, arrows or circles, which this tool can be used for drawing such things to screenshot images called from within automation scripts used  for compiling documentation.


ImageProcessor usage:

    ImageProcessor <PathToImageFile> [Command1 [ParameterList1]] [Command2 [ParameterList2]] [Command...]]...

    aviable commands:


        cuttenEdges   : Removes [bordersize] pixels from the image's edges (for removing background
                        which may be visible at the edges, behind windows.) when used without parameters,
                        it will use 3 1 3 3 for "left top right bottom" by derfault.
                        usage:
                                cuttenEdges [borderSize|<left top right bottom>]

        cropFrame     : Crops the area [X Y Width Height] out of the image by discarding
                        anything else which would be outside this defined rectangle.
                        usage:
                                cropFrame <X Y W H>

        roundCorners  : Roundens a taken image's corners (by default 4,5 pixel) or by given parameter.
                        When given a second parameter, first parameter will be used for cutting
                        the upper corners, and the second parameter for cutting the lower corners
                        usage:
                                roundCorners [CornerSize|UpperCorners LowerCorners]

        rotateImage   : Rotates the whole image for N steps (-90° per step). also
                        giving absolute degree values of 90°, 180° or 270° is supported:
                        usage:
                                rotateImage [+/-]<N> | [+/-]<90|180|270>

        flipImage     : Flips the whole image eitrher 'UP' direction or 'RIGHT' direction:
                        usage:
                                flipImage <UP|RIGHT>

        inlineImage   : Draws an inline at <bordersize> pixel strange round the inner edges of the image at actual set color
                        usage:
                                inlineImage <bordersize>

        outlineImage  : Draws an outline at <bordersize> thickness round the image's outer edges, at actual set color
                        usage:
                                outlineImage <bordersize>

        setColor      : Set the color to be used for drawing markers:
                        takes 1x int-value (hex or dec) or 3 to 4 separated byte-values:
                        usage:
                                setColor <rgb|rgba|R G B [A]>

                        Also possible are 'modifiers' for changing the actual loaded color state
                        like:
                                setColor <Dark|Light|Soft|Opaque|Invert|Accent>

                        Also supported are strings representing Named colors as like 'Red', 'Blue'
                        'Orange', or 'Pink' (Case-Insensitive). As well combinations of these are
                        possible and may be given dot '.' separated, like such this
                        color:
                                setColor dark.Green.gray  or setColor orange.deep.Red

        drawLine      : Draws a line from point <X1 Y1> to point <X2 Y2> at optional strength [S]:
                        usage:
                                drawLine <X1 Y1 X2 Y2> [S]

        drawMarker    : Draws a marker overlay to the loaded Image
                        As first parameter can be given a 'SHAPE' argument which decides how any
                        following number parameters will be interpreted for drawing the marker:
                         - ARROW (followed by 4 parameters) will draw an arrow pointing from P1/P2 to P3/P4.
                         - CROSS (followed by 3 parameters) will draw a crosshair at P1/P2 by radius P3.
                         - CIRCLE (followed by 4 parameters) draws an ellypsoid, with center at P1/P2 at size P3/P4.
                         - SQUARE (followed by 3 parameters) draws a square rectangle (top-left: P1/P2, size: P3)
                        If the SHAPE parameter is ommitted, the command then will 'guess' a SHAPE
                        on it's own by the count on positioning parameters which may follow:
                          - 2 parameters are interpreted as: Arrow (pointing good visible to point P1/P2).
                          - 3 parameters are interpreted as: Circle (centered at P1/P2 with radius P3).
                          - 4 parameters are interpreted as: Rectangle (top-left corner P1/P2 with size P3/P4)
                        usage:
                                drawMarker [SHAPE] <posX> <posY> [width [height]]

        showObject    : Shows up an object within an image by pointing it with an ARROW marker
                        at clearly visible size and position. If given 2 parameters, and point
                        lays inside last drawn CIRCLE or SQUARE marker area, it will point it's
                        area's center position then. If point does not lay within last
                        drawn area, it will just point out the given coordinates directly.
                        If given 4 parameters, it will draw a marker [X Y W H] plus
                        an ARROW pointing that last SHAPE markers center position then by
                        using complement of the color which is currently sellected drawing color.
                        usage:
                                showObject [SHAPE] <posX> <posY> [Width [Height]]

        dragObject    : Visulizes a planed 'drag' operation and (if maybe different) the actual
                        result of a planed drag operation when it was completed at least.
                        Takes a SHAPE parameter and positioning parameter P1/P2 to P3/P4. If P1/P2 lays
                        inside a prior (directly before) drawn CIRCLE or SQUARE marker, it will use
                        that marker's center position then as the 'dragling' instead
                        usage:
                                dragObject [SHAPE] <fromX> <fromY> <toX> <toY>


Within a single ImageProcessor call, as many commands it maybe needs for completing a desired
screenshot image can be chained after each other. here a short command line example:

    ImageProcessor someImageFile.png cuttenEdges roundCorners 2,5 0 drawMarker 100 75 50 setColor 255 128 0 drawMarker 300 250 400 270

above command line loads an image, then cuts 3 pixels from each of it's edges, rounds it's
upper-left and upper-right corners by radius of 2.5pixel and then it draws a red circle by
radius 50px at position x:100 y:75 and an orange rectangle by width/height 400 X 270 pixel
at position x:300 y:250.



    
    
