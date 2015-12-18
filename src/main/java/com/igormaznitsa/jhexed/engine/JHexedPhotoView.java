package com.igormaznitsa.jhexed.engine;

import android.content.Context;
import android.content.res.AssetManager;
import android.view.MotionEvent;
import android.view.View;

import com.apphunt.app.R;
import com.igormaznitsa.jhexed.engine.misc.HexPoint2D;
import com.igormaznitsa.jhexed.engine.misc.HexPosition;
import com.igormaznitsa.jhexed.engine.misc.HexRect2D;
import com.igormaznitsa.jhexed.engine.renders.HexEngineRender;

import java.io.IOException;
import java.util.List;
import java.util.Random;

/**
 * A Custom View example to show possibility of JHexed (https://code.google.com/p/jhexed/) usage with Android.
 */
public class JHexedPhotoView extends View implements HexEngineRender<Canvas> {

    /**
     * The Width of the hex cell in pixels.
     */
    private final int cellWidth;

    /**
     * The Height of the hex cell in pixels.
     */
    private final int cellHeight;

    /**
     * The Width of the hex field in cells.
     */
    private final int fieldWidth;

    /**
     * The Height of the hex field in cells.
     */
    private final int fieldHeight;

    /**
     * Width of the hex cell border line.
     */
    private final float hexLineWidth;

    /**
     * The Color of border of an unselected hex cell.
     */
    private final int hexEdgeColor;

    /**
     * The Color of border of a selected hex cell.
     */
    private final int hexSelectedEdgeColor;

    /**
     * The JHexed engine for hexagonal math.
     */
    private final HexEngine<Canvas> engine;

    /**
     * The Paint to draw hexagons.
     */
    private final Paint theHexPaint;

    /**
     * The Model if the data source for JHexed engine.
     */
    private final DefaultIntegerHexModel hexModel;
    /**
     * Array contains the original loaded icons which scaled to cell size
     */
    private final Bitmap[] icons;
    /**
     * Array contains prepared and optimized icons to be drawn in hexagons
     */
    private final Bitmap[] iconsOptimizedForHexagons;
    private final Context context;
    /**
     * The Hexagonal Path.
     */
    private Path hexPath;
    /**
     * X Offset of visible area.
     */
    private float screenOffsetX = 20;
    /**
     * Y offset of visible area.
     */
    private float screenOffsetY = 50;
    /**
     * The Last X coordinate of pointer.
     */
    private float lastPointerX;
    /**
     * The Last Y coordinate of pointer.
     */
    private float lastPointerY;
    /**
     * Flag shows that the DRAG mode is active;
     */
    private boolean dragModeActive;
    /**
     * The Selected Hexagon position.
     */
    private HexPosition selectedHexPosition;

    public JHexedPhotoView(final Context ctx, List<Bitmap> icons) {
        super(ctx);
        context = ctx;
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        this.icons = icons.toArray(new Bitmap[icons.size()]);

        // Read properties of the View
        this.cellWidth = 94;
        this.cellHeight = 84;
        this.fieldWidth = 82;
        this.fieldHeight = 82;
        this.hexEdgeColor = Color.WHITE;
        this.hexSelectedEdgeColor = Color.BLUE;
        this.hexLineWidth = 4.0f;

        // Create and fill the model by random values for loaded icon number
        this.hexModel = new DefaultIntegerHexModel(this.fieldWidth, this.fieldHeight, -1);
        if (!this.isInEditMode()) {
            final Random rnd = new Random();
            for (int c = 0; c < this.fieldWidth; c++) {
                for (int r = 0; r < this.fieldHeight; r++) {
                    this.hexModel.setValueAt(c, r, rnd.nextInt(this.icons.length));
                }
            }
        }

        // Prepare the Paint styple to draw hexagon borders
        theHexPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        theHexPaint.setStyle(Paint.Style.STROKE);
        theHexPaint.setStrokeWidth(this.hexLineWidth);

        // Create and init the hexagonal engine
        this.engine = new HexEngine<Canvas>(this.cellWidth, this.cellHeight, HexEngine.ORIENTATION_HORIZONTAL);
        this.engine.setModel(this.hexModel);
        this.engine.setRenderer(this);

        // Make the Path to draw hex
        updateCurrentHexPath();

        // Prepare the hexagonal icons to be drawn in the Path
        this.iconsOptimizedForHexagons = makeHexagonalIcons(this.icons, this.hexPath);
    }

    /**
     * Prepare icons to be drawn in a hexagons.
     *
     * @param icons   an array contains original icons, must not be null
     * @param hexPath a Path describes a hexagon, must not be null
     * @return an array contains resized and masked icons to be drawn in hexagons
     */
    private Bitmap[] makeHexagonalIcons(final Bitmap[] icons, final Path hexPath) {
        final Bitmap[] result = new Bitmap[icons.length];

        final int maskColor = 0xff424242;
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(maskColor);

        final Xfermode xferMode = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);

        final RectF pathBounds = new RectF();
        hexPath.computeBounds(pathBounds, true);

        final int icoWidth = Math.round(pathBounds.width());
        final int icoHeight = Math.round(pathBounds.height());

        int index = 0;
        for (final Bitmap b : icons) {
            Bitmap source = b;
            if (source.getWidth() != icoWidth || source.getHeight() != icoHeight) {
                source = Bitmap.createScaledBitmap(source, icoWidth, icoHeight, false);
            }

            final Bitmap newBitmap = Bitmap.createBitmap(icoWidth, icoHeight, Bitmap.Config.ARGB_8888);
            final Canvas newBitmapCanvas = new Canvas(newBitmap);
            newBitmapCanvas.drawARGB(0, 0, 0, 0);
            paint.setXfermode(null);
            newBitmapCanvas.drawPath(hexPath, paint);
            paint.setXfermode(xferMode);
            newBitmapCanvas.drawBitmap(source, 0, 0, paint);
            result[index++] = newBitmap;
        }

        return result;
    }

    /**
     * Load image icons from asset.
     *
     * @param ctx             the current view context, must not be null
     * @param assetImageNames array of asset image names, must not be null
     * @return loaded array of icons as Bitmaps
     * @throws IOException it will be thrown if error during loading
     */
    private static Bitmap[] loadIconsFromAsset(final Context ctx, final String[] assetImageNames) throws IOException {
        final Bitmap[] result = new Bitmap[assetImageNames.length];
        final AssetManager assetManager = ctx.getAssets();
        int index = 0;
        for (final String s : assetImageNames) {
            //final InputStream in = assetManager.open(s);
            try {
                final Bitmap loaded = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_logo);
                result[index++] = loaded;
            } finally {
                //in.close();
            }
        }
        return result;
    }

    /**
     * Get info about hexagon from engine and recalculate Path
     */
    private void updateCurrentHexPath() {
        final Path newPath = new Path();

        final HexPoint2D[] points = this.engine.getHexScaledPoints();

        newPath.moveTo(points[0].getX(), points[0].getY());
        newPath.lineTo(points[1].getX(), points[1].getY());
        newPath.lineTo(points[2].getX(), points[2].getY());
        newPath.lineTo(points[3].getX(), points[3].getY());
        newPath.lineTo(points[4].getX(), points[4].getY());
        newPath.lineTo(points[5].getX(), points[5].getY());
        newPath.lineTo(points[0].getX(), points[0].getY());

        this.hexPath = newPath;
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        Rect visibleRect = new Rect();
        if (this.getGlobalVisibleRect(visibleRect)) {
            this.engine.drawArea(canvas, new HexRect2D(visibleRect.left - 60, visibleRect.top - 60, visibleRect.width(), visibleRect.height()), true);
            //drawSelection(canvas);
        } else {
            canvas.getClipBounds(visibleRect);
            this.engine.drawArea(canvas, new HexRect2D(visibleRect.left - 60, visibleRect.top - 60, visibleRect.width(), visibleRect.height()), true);
        }
    }

    /**
     * Draw selection if presented
     *
     * @param canvas a canvas to draw
     */
    private void drawSelection(final Canvas canvas) {
        if (this.selectedHexPosition != null) {
            final float x = this.engine.calculateX(this.selectedHexPosition.getColumn(), this.selectedHexPosition.getRow()) - this.screenOffsetX;
            final float y = this.engine.calculateY(this.selectedHexPosition.getColumn(), this.selectedHexPosition.getRow()) - this.screenOffsetY;

            this.theHexPaint.setColor(this.hexSelectedEdgeColor);

            final Matrix matrix = new Matrix();
            matrix.setTranslate(x, y);
            canvas.setMatrix(matrix);
            canvas.drawPath(this.hexPath, theHexPaint);
        }
    }

    @Override
    public void renderHexCell(final HexEngine<Canvas> canvasHexEngine, final Canvas canvas, final float x, final float y, final int col, final int row) {
        final Matrix matrix = new Matrix();
        matrix.setTranslate(x - screenOffsetX, y - screenOffsetY);
//        matrix.setTranslate(x - screenOffsetX, y);
        this.theHexPaint.setColor(this.hexEdgeColor);
        canvas.setMatrix(matrix);

        int position = this.hexModel.getValueAt(col, row);
        if (!this.isInEditMode() && position  >= 0 && position < iconsOptimizedForHexagons.length) {
            final Bitmap icon = this.iconsOptimizedForHexagons[position];
            canvas.drawBitmap(icon, 0f, 0f, theHexPaint);
        }
        canvas.drawPath(this.hexPath, theHexPaint);
    }

    @Override
    public void attachedToEngine(final HexEngine<?> hexEngine) {

    }

    @Override
    public void detachedFromEngine(final HexEngine<?> hexEngine) {

    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
//        switch (event.getAction()) {
//            case MotionEvent.ACTION_DOWN: {
//                this.lastPointerX = event.getX();
//                this.lastPointerY = event.getY();
//                this.dragModeActive = true;
//
//                // find selected
//                final float absPointX = this.lastPointerX + this.screenOffsetX;
//                final float absPointY = this.lastPointerY + this.screenOffsetY;
//
//                final HexPosition pressedHex = this.engine.pointToHex(absPointX, absPointY);
//
//                this.selectedHexPosition = pressedHex.isAtModel(this.hexModel) ? pressedHex : null;
//
//                this.invalidate();
//            }
//            break;
//            case MotionEvent.ACTION_UP:
//            case MotionEvent.ACTION_MOVE: {
//                if (this.dragModeActive) {
//                    final float dx = lastPointerX - event.getX();
//                    final float dy = lastPointerY - event.getY();
//
//                    this.screenOffsetX = this.screenOffsetX + Math.round(dx);
//                    this.screenOffsetY = this.screenOffsetY + Math.round(dy);
//                    this.lastPointerX = event.getX();
//                    this.lastPointerY = event.getY();
//
//                    this.invalidate();
//                }
//                if (event.getAction() == MotionEvent.ACTION_UP) {
//                    dragModeActive = false;
//                }
//            }
//            break;
//        }

        return true;
    }
}