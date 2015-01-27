/// package's name
package edu.gcsc.vrl.swcdensityvis.types;

/// imports
import java.io.Serializable;
import org.la4j.matrix.dense.Basic2DMatrix;

/**
 * @brief the matrix class
 * @author stephan grein
 */
public class Dense4x4Matrix extends Basic2DMatrix implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * @brief default ctor
	 */
	public Dense4x4Matrix() {
		super(4, 4);
	}
}
