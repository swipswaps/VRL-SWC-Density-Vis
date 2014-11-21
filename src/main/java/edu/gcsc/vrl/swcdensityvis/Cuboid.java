package edu.gcsc.vrl.swcdensityvis;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @brief a cuboid class
 * @author stephan
 */
@ToString @EqualsAndHashCode @AllArgsConstructor @Getter @Setter public class Cuboid {
	private final float x;
	private final float y;
	private final float z;
	private final float width;
	private final float height;
	private final float depth;
}