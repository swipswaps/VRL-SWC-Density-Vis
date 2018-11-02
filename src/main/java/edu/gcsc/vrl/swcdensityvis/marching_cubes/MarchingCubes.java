/// package's name
package edu.gcsc.vrl.swcdensityvis.marching_cubes;

/// imports
import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import edu.gcsc.vrl.densityvis.VoxelSet;
import edu.gcsc.vrl.swcdensityvis.importer.DensityVisualizable;
import eu.mihosoft.vrl.reflection.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TriangleArray;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

/**
 * @brief Parallel Marching Cubes with float precision
 * @author stephanmg <stephan@syntaktischer-zucker.de>
 */
public class MarchingCubes {
	/// lerp
	private static float[] lerp(float[] vec1, float[] vec2, float alpha) {
		return new float[]{vec1[0] + (vec2[0] - vec1[0]) * alpha, vec1[1] + (vec2[1] - vec1[1]) * alpha, vec1[2] + (vec2[2] - vec1[2]) * alpha};
	}

	/// marching cubes
	private static void marchingCubesFloat(float[] values, int[] volDim, int volZFull, float[] voxDim, float isoLevel, int offset, CallbackMC callback) {
		ArrayList<Point3f> vertices = new ArrayList<Point3f>();
		// Actual position along edge weighted according to function values.
		float vertList[][] = new float[12][3];

		// Calculate maximal possible axis value (used in vertice normalization)
		float maxX = voxDim[0] * (volDim[0] - 1);
		float maxY = voxDim[1] * (volDim[1] - 1);
		float maxZ = voxDim[2] * (volZFull - 1);

		// Volume iteration
		for (int z = 0; z < volDim[2] - 1; z++) {
			for (int y = 0; y < volDim[1] - 1; y++) {
				for (int x = 0; x < volDim[0] - 1; x++) {
                   			// Indices pointing to cube vertices
					//              pyz  ___________________  pxyz
					//                  /|                 /|
					//                 / |                / |
					//                /  |               /  |
					//          pz   /___|______________/pxz|
					//              |    |              |   |
					//              |    |              |   |
					//              | py |______________|___| pxy
					//              |   /               |   /
					//              |  /                |  /
					//              | /                 | /
					//              |/__________________|/
					//             p                     px
					int p = x + (volDim[0] * y) + (volDim[0] * volDim[1] * (z + offset)),
						px = p + 1,
						py = p + volDim[0],
						pxy = py + 1,
						pz = p + volDim[0] * volDim[1],
						pxz = px + volDim[0] * volDim[1],
						pyz = py + volDim[0] * volDim[1],
						pxyz = pxy + volDim[0] * volDim[1];

					//				  X              Y                    Z
					float position[] = new float[]{x * voxDim[0], y * voxDim[1], (z + offset) * voxDim[2]};
					// Voxel intensities
					float value0 = values[p],
						value1 = values[px],
						value2 = values[py],
						value3 = values[pxy],
						value4 = values[pz],
						value5 = values[pxz],
						value6 = values[pyz],
						value7 = values[pxyz];

					// Voxel is active if its intensity is above isolevel
					int cubeindex = 0;
					if (value0 > isoLevel) {
						cubeindex |= 1;
					}
					if (value1 > isoLevel) {
						cubeindex |= 2;
					}
					if (value2 > isoLevel) {
						cubeindex |= 8;
					}
					if (value3 > isoLevel) {
						cubeindex |= 4;
					}
					if (value4 > isoLevel) {
						cubeindex |= 16;
					}
					if (value5 > isoLevel) {
						cubeindex |= 32;
					}
					if (value6 > isoLevel) {
						cubeindex |= 128;
					}
					if (value7 > isoLevel) {
						cubeindex |= 64;
					}

					// Fetch the triggered edges
					int bits = LookupTables.EDGE_TABLE[cubeindex];

					// If no edge is triggered... skip
					if (bits == 0) {
						continue;
					}

					// Interpolate the positions based on voxel intensities
					float mu = 0.5f;

					// bottom of the cube
					if ((bits & 1) != 0) {
						mu = ((isoLevel - value0) / (value1 - value0));
						vertList[0] = lerp(position, new float[]{position[0] + voxDim[0], position[1], position[2]}, mu);
					}
					if ((bits & 2) != 0) {
						mu = ((isoLevel - value1) / (value3 - value1));
						vertList[1] = lerp(new float[]{position[0] + voxDim[0], position[1], position[2]}, new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2]}, mu);
					}
					if ((bits & 4) != 0) {
						mu = ((isoLevel - value2) / (value3 - value2));
						vertList[2] = lerp(new float[]{position[0], position[1] + voxDim[1], position[2]}, new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2]}, mu);
					}
					if ((bits & 8) != 0) {
						mu = ((isoLevel - value0) / (value2 - value0));
						vertList[3] = lerp(position, new float[]{position[0], position[1] + voxDim[1], position[2]}, mu);
					}
					// top of the cube
					if ((bits & 16) != 0) {
						mu = ((isoLevel - value4) / (value5 - value4));
						vertList[4] = lerp(new float[]{position[0], position[1], position[2] + voxDim[2]}, new float[]{position[0] + voxDim[0], position[1], position[2] + voxDim[2]}, mu);
					}
					if ((bits & 32) != 0) {
						mu = ((isoLevel - value5) / (value7 - value5));
						vertList[5] = lerp(new float[]{position[0] + voxDim[0], position[1], position[2] + voxDim[2]}, new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2] + voxDim[2]}, mu);
					}
					if ((bits & 64) != 0) {
						mu = ((isoLevel - value6) / (value7 - value6));
						vertList[6] = lerp(new float[]{position[0], position[1] + voxDim[1], position[2] + voxDim[2]}, new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2] + voxDim[2]}, mu);
					}
					if ((bits & 128) != 0) {
						mu = ((isoLevel - value4) / (value6 - value4));
						vertList[7] = lerp(new float[]{position[0], position[1], position[2] + voxDim[2]}, new float[]{position[0], position[1] + voxDim[1], position[2] + voxDim[2]}, mu);
					}
					// vertical lines of the cube
					if ((bits & 256) != 0) {
						mu = ((isoLevel - value0) / (value4 - value0));
						vertList[8] = lerp(position, new float[]{position[0], position[1], position[2] + voxDim[2]}, mu);
					}
					if ((bits & 512) != 0) {
						mu =  ((isoLevel - value1) / (value5 - value1));
						vertList[9] = lerp(new float[]{position[0] + voxDim[0], position[1], position[2]}, new float[]{position[0] + voxDim[0], position[1], position[2] + voxDim[2]}, mu);
					}
					if ((bits & 1024) != 0) {
						mu = ((isoLevel - value3) / (value7 - value3));
						vertList[10] = lerp(new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2]}, new float[]{position[0] + voxDim[0], position[1] + voxDim[1], position[2] + voxDim[2]}, mu);
					}
					if ((bits & 2048) != 0) {
						mu = ((isoLevel - value2) / (value6 - value2));
						vertList[11] = lerp(new float[]{position[0], position[1] + voxDim[1], position[2]}, new float[]{position[0], position[1] + voxDim[1], position[2] + voxDim[2]}, mu);
					}

					// construct triangles -- get correct vertices from triTable.
					int i = 0;
					// "Re-purpose cubeindex into an offset into triTable."
					cubeindex <<= 4;

					while (LookupTables.TRIANGLE_TABLE[cubeindex + i] != -1) {
						int index1 = LookupTables.TRIANGLE_TABLE[cubeindex + i];
						int index2 = LookupTables.TRIANGLE_TABLE[cubeindex + i + 1];
						int index3 = LookupTables.TRIANGLE_TABLE[cubeindex + i + 2];

						// Add triangle vertices unnormalized
						vertices.add(new Point3f(vertList[index3][0], vertList[index3][1], vertList[index3][2]));
						vertices.add(new Point3f(vertList[index2][0], vertList[index2][1], vertList[index2][2]));
						vertices.add(new Point3f(vertList[index1][0], vertList[index1][1], vertList[index1][2]));
						i += 3;
					}
				}
			}
		}
		callback.setVertices(vertices);
		callback.run();
	}
	
	/**
	 * @brief Callback for Marching Cubes
	 */
	abstract class CallbackMC implements Runnable {
		private ArrayList<Point3f> vertices;

		@SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
		void setVertices(ArrayList<Point3f> vertices) {
			this.vertices = vertices;
		}

		@SuppressWarnings("ReturnOfCollectionOrArrayField")
		ArrayList<Point3f> getVertices() {
			return this.vertices;
		}
	}

	/**
	 * @brief creates a scalar field for the marching cubes algorithm
	 * @param voxels
	 * @param visualizer
	 * @param scale
	 * Note: To ensure consistency one needs to iterate in the same way over
	 * the density data as the DensityComputationStrategies do, e.g. see the
	 * triple for loop in TreeDensityComputationStrategy in the XML package.
	 * Also, it is assumed that x, y and z dimensions of voxels are the same.
	 * @return 
	 */
	private static float[] createScalarField(List<? extends VoxelSet> voxels, DensityVisualizable visualizer, float scale) {
		Vector3f center = (Vector3f) visualizer.getCenter();
		Vector3f dim = (Vector3f) visualizer.getDimension();
		dim.x *= 1f/scale;
		dim.y *= 1f/scale;
		dim.z *= 1f/scale;
		int index = 0;
		final float[] scalarField = new float[voxels.size() * (int) dim.x / voxels.get(0).getWidth()]; 
		for (float x = center.x - dim.x/2; x < center.x + dim.x/2; x += voxels.get(0).getWidth()) {
			for (float y = center.y - dim.y/2; y < center.y + dim.y/2; y += voxels.get(0).getHeight()) {
				for (float z = center.z - dim.z/2; z < center.z + dim.z/2; z += voxels.get(0).getDepth()) {
					scalarField[index] = (float) voxels.get(index).getValue();
					index++;
				}
			}
		}
		System.err.println("scalarField length (created from voxel data): " + scalarField.length);
		return scalarField;
	}


	/**
	 * @brief the marching cubes for our density voxels
	 * @param voxels
	 * @param visualizer
	 * @param scale
	 * @param offset
	 * @param isovalue
	 * @return 
	 */
	@SuppressWarnings("CallToPrintStackTrace")
	public Shape3D MC(List<? extends VoxelSet> voxels, DensityVisualizable visualizer, float scale, float isovalue) {
		/// dimension of density data
		Vector3f dim = (Vector3f) visualizer.getDimension();
		float width = dim.x/scale;
		float depth = dim.y/scale;
		float height = dim.z/scale;

		/// dimension of individual voxels (Note: Could be improved)
		float voxWidth = voxels.get(0).getWidth();
		float voxHeight = voxels.get(0).getHeight();
		float voxDepth = voxels.get(0).getDepth();

		/// size of scalar field
		final int sizeX = (int) (width / voxWidth);
		final int sizeY = (int) (depth / voxHeight);
		final int sizeZ = (int) (height / voxDepth);

		/// the scalar field for our voxel data
		final float[] scalarField = createScalarField(voxels, visualizer, scale);
		final float[] voxDim = new float[]{voxWidth, voxHeight, voxDepth};
		
		/// the iso level which should be visualized
		final float isoLevel = isovalue;
		
		/// the number of threads (Note: Includes hyperthreading)
		int nThreads = Runtime.getRuntime().availableProcessors();
		ArrayList<Thread> threads = new ArrayList<Thread>();

		/// the resulting array
		final ArrayList<ArrayList<Point3f>> results = new ArrayList<ArrayList<Point3f>>();

		/// distribute thread work depending on number of threads 
		int remainder = sizeX % nThreads;
		int segment = sizeX / nThreads;

		/// partition parallel work
		int zAxisOffset = 0;
		for (int i = 0; i < nThreads; i++) {
			/// distribute remainder among first (remainder) threads
			@SuppressWarnings("ValueOfIncrementOrDecrementUsed")
			int segmentSize = (remainder-- > 0) ? segment + 1 : segment;

			/// padding needs to be added to correctly close the gaps between segments
			final int paddedSegmentSize = (i != nThreads - 1) ? segmentSize + 1 : segmentSize;

			/// finished callback
			final CallbackMC callback = new CallbackMC() {
				@Override
				public void run() {
					results.add(getVertices());
				}
			};

			final int finalZAxisOffset = zAxisOffset;

			/// create thread runnable
			Thread t = new Thread() {
				@Override
				public void run() {
					MarchingCubes.marchingCubesFloat(scalarField, new int[]{sizeX, sizeY, paddedSegmentSize}, sizeZ, voxDim, isoLevel, finalZAxisOffset, callback);
				}
			};

			/// add thread to list and start
			threads.add(t);
			t.start();

			/// correct offsets for next iteration
			zAxisOffset += segmentSize;
		}

		/// join all threads
		for (int i = 0; i < threads.size(); i++) {
			try {
				threads.get(i).join();
			} catch (InterruptedException e) {
				/// Note: Could improve error message
				System.err.println("Threads could not be joined.");
				e.printStackTrace();
			}
		}

		/// debug information: total number of verts
		int totalVerts = 0;
		for (int i = 0; i < results.size(); i++) {
			totalVerts += results.get(i).size();
		}
		System.err.println("total vertices (of isocontours): " + totalVerts);

		/// fill linear 1d list with all vertices of surface
		ArrayList<Point3f> vertices = new ArrayList<Point3f>();
		for (int i = 0; i < results.size(); i++) {
			vertices.addAll(results.get(i));
		}

		/// Vertices are unscaled here, because we take the raw unscaled 
		/// density as an input from ComputeDensity. This has to be then
		/// rescaled with the scale value since we want to visualize the
		/// resulting density, geometry and isocontours on the Canvas.
		for (int i = 0; i < vertices.size(); i++) {
			vertices.get(i).x *= scale;
			vertices.get(i).y *= scale;
			vertices.get(i).z *= scale;
		}
		
		/// Get bounding box of voxels' density
		@SuppressWarnings("unchecked")
		Pair<Vector3f, Vector3f> bb = (Pair<Vector3f, Vector3f>)visualizer.getBoundingBox();
		Vector3f min_bb = bb.getFirst();
		min_bb.x *= scale;
		min_bb.y *= scale;
		min_bb.z *= scale;
		
		/// Can optimize here: The bounding box min of vertices of MC 
		/// isocontours will always be at the coordinates M(0, 0, 0).
		Vector3f min = getMin(vertices);
		float shift_x = (min_bb.x - min.x)/scale;
		float shift_y = (min_bb.y - min.y)/scale;
		float shift_z = (min_bb.z - min.z)/scale;
		
		/// Shift now the MC vertices towards the density voxels
		for (int i = 0; i < vertices.size(); i++) {
			vertices.get(i).x += shift_x;
			vertices.get(i).y += shift_y;
			vertices.get(i).z += shift_z;
		}

		/// create Java3D triangles with normal information and colors
		TriangleArray triArray = new TriangleArray(vertices.size(), TriangleArray.COORDINATES 
			| TriangleArray.NORMALS | TriangleArray.COLOR_3);
		triArray.setCoordinates(0, vertices.toArray(new Point3f[vertices.size()]));
		
		GeometryInfo geometryInfo = new GeometryInfo(triArray);
		NormalGenerator ng = new NormalGenerator();
		ng.generateNormals(geometryInfo);
		/// Note: For illumination model, need to get gradient of scalarfield
		GeometryArray result = geometryInfo.getGeometryArray();
		return new Shape3D(result);
	}
	
	/**
	 * @brief get minimum coordinate of a set of vertices,
	 * Note: If the set is empty, then (0, 0, 0) is returned.
	 * @param vertices
	 * @return 
	 */
	public static Vector3f getMin(ArrayList<Point3f> vertices) {
		if (vertices.isEmpty()) {
			return new Vector3f(0f, 0f, 0f);
		}
		
		ArrayList<Float> temp_x = new ArrayList<Float>();
		ArrayList<Float> temp_y = new ArrayList<Float>();
		ArrayList<Float> temp_z = new ArrayList<Float>();
		for (Point3f p : vertices) {
			temp_x.add(p.x);
			temp_y.add(p.y);
			temp_z.add(p.z);
		}
		
		return new Vector3f(Collections.min(temp_x), Collections.min(temp_y), Collections.min(temp_z));
	}
}
