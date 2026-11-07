package net.drehtuer.nextgallery.core.model.edit

/** Maps normalized coordinates through operations, so that operations made one after the other compose. */
object GeometryMapper {
    private const val QUARTER_TURNS = 4

    /** From the coordinates before [operation] to those after it. Scaling and drawing do not move anything. */
    fun matrixFor(operation: EditOperation): Matrix3 = when (operation) {
        is EditOperation.Rotate -> rotation(operation.quarterTurns)
        is EditOperation.Crop -> crop(operation.rect)
        is EditOperation.Scale, is EditOperation.PenStroke -> Matrix3.IDENTITY
    }

    fun matrixThrough(operations: List<EditOperation>): Matrix3 =
        operations.fold(Matrix3.IDENTITY) { matrix, operation -> matrix.then(matrixFor(operation)) }

    /** The result lies outside 0..1 if a crop in [operations] removed the point. */
    fun mapPoint(point: NormalizedPoint, operations: List<EditOperation>): PlanePoint =
        matrixThrough(operations).map(point.x.toDouble(), point.y.toDouble())

    fun rotation(quarterTurns: Int): Matrix3 = when (quarterTurns.mod(QUARTER_TURNS)) {
        1 -> Matrix3(0.0, -1.0, 1.0, 1.0, 0.0, 0.0)
        2 -> Matrix3(-1.0, 0.0, 1.0, 0.0, -1.0, 1.0)
        QUARTER_TURNS - 1 -> Matrix3(0.0, 1.0, 0.0, -1.0, 0.0, 1.0)
        else -> Matrix3.IDENTITY
    }

    fun crop(left: Double, top: Double, width: Double, height: Double): Matrix3 =
        Matrix3(1 / width, 0.0, -left / width, 0.0, 1 / height, -top / height)

    private fun crop(rect: NormalizedRect): Matrix3 = crop(
        rect.left.toDouble(),
        rect.top.toDouble(),
        rect.width.toDouble(),
        rect.height.toDouble(),
    )
}
