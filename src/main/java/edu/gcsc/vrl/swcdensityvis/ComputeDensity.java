/// package's name
package edu.gcsc.vrl.swcdensityvis;

/// imports
import edu.gcsc.vrl.swcdensityvis.importer.SWC.SWCCompartmentInformation;
import edu.gcsc.vrl.swcdensityvis.util.SWCUtility;
import edu.gcsc.vrl.densityvis.Density;
import edu.gcsc.vrl.densityvis.DensityResult;
import edu.gcsc.vrl.swcdensityvis.DensityUtil;
import edu.gcsc.vrl.swcdensityvis.importer.DensityComputationContext;
import edu.gcsc.vrl.swcdensityvis.importer.DensityComputationStrategyFactoryProducer;
import edu.gcsc.vrl.swcdensityvis.importer.DensityVisualizable;
import edu.gcsc.vrl.swcdensityvis.importer.DensityVisualizableFactory;
import edu.gcsc.vrl.swcdensityvis.importer.SWC.SWCCompartmentInformation;
import edu.gcsc.vrl.swcdensityvis.importer.XML.XMLDensityUtil;
import edu.gcsc.vrl.swcdensityvis.importer.XML.XMLDensityVisualizer;
import eu.mihosoft.vrl.annotation.ComponentInfo;
import eu.mihosoft.vrl.annotation.MethodInfo;
import eu.mihosoft.vrl.annotation.OutputInfo;
import eu.mihosoft.vrl.annotation.ParamGroupInfo;
import eu.mihosoft.vrl.annotation.ParamInfo;
import eu.mihosoft.vrl.v3d.VTriangleArray;
import eu.mihosoft.vrl.v3d.jcsg.Cube;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import static javax.media.j3d.GeometryArray.COORDINATES;
import javax.media.j3d.LineArray;
import javax.vecmath.Vector3f;

/**
 * @brief computes the density
 */
@ComponentInfo(name = "ComputeDensity", category = "Neuro/SWC-Density-Vis")
public class ComputeDensity implements java.io.Serializable {

	private static final long serialVersionUID = 1L;

	@MethodInfo(valueStyle = "multi-out", interactive = false)
	@OutputInfo(
		style = "multi-out",
		elemNames = {"Density", "Geometry"},
		elemTypes = { DensityResult.class, HashMap.class }
		)
	public Object[] compute(
		@ParamGroupInfo(group = "Common options|true|Compute the density for the image (stack); Folder|true|Input folder")
		@ParamInfo(name = "Input folder", typeName = "Location of SWC files", style = "load-folder-dialog", options = "endings=[\"swc\", \"xml\", \"asc\"]; description=\"SWC, XML or ASC files (.swc, .xml, .asc)\"") File folder,
		@ParamInfo(name = "File type", typeName = "Filetype", style="selection", options="value=[\"SWC\", \"XML\", \"ASC\"") String selection,
		@ParamGroupInfo(group = "Common options|true|Compute the density for the image (stack); Dimensions|true|Dimensions")
		@ParamInfo(name = "Width", typeName = "Width of sampling cube", style = "slider", options = "min=1;max=100") int width,
		@ParamGroupInfo(group = "Common options|true|Compute the density for the image (stack); Dimensions|true|Dimensions")
		@ParamInfo(name = "Height", typeName = "Height of sampling cube", style = "slider", options = "min=1;max=100") int height,
		@ParamGroupInfo(group = "Common options|true|Compute the density for the image (stack); Dimensions|true|Dimensions")
		@ParamInfo(name = "Depth", typeName = "Depth of sampling cube", style = "slider", options = "min=1;max=100") int depth,
		@ParamGroupInfo(group = "Advanced options|true|Compute the density for the image (stack); Compartment|true|Compartment")
		@ParamInfo(name = "Type", typeName = "Compartment", style = "selection", options = "value=[\"all\", \"undefined\", \"axon\", \"(basal) dendrite\", \"apical dendrite\", \"fork point\", \"end point\", \"custom\"]") String choice
	) {
		HashMap<String, ArrayList<SWCCompartmentInformation>> cells = new HashMap<String, ArrayList<SWCCompartmentInformation>>();
		try {
			File[] swcFiles = folder.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".swc") ||
				               name.endsWith(".xml") ||
					       name.endsWith(".asc");
				}
			});

		DensityVisualizableFactory factory = new DensityVisualizableFactory();
		/**
		 * @todo factory needs to be enhanced by the XMLDensityUtil maybe (i. e. DensityVisulziable)
		 */
		DensityVisualizable visualizer = factory.getDensityVisualizer(selection);
		DensityComputationContext densityComputationContext = new DensityComputationContext();
		densityComputationContext.setDensityComputationStrategy(new DensityComputationStrategyFactoryProducer().getDefaultAbstractDensityComputationStrategyFactory().getDefaultComputationStrategy(selection));
		visualizer.setContext(densityComputationContext);
		
		XMLDensityVisualizer xmlDensityVisualizer = new XMLDensityVisualizer(XMLDensityUtil.getDefaultImpl());
		xmlDensityVisualizer.setContext(densityComputationContext);
		
		/**
		 * @todo make operations with the xmldensityVisualizer
		 */

			
			for (File f : swcFiles) {
				eu.mihosoft.vrl.system.VMessage.info("Parsing SWC file", f.toString());
				cells.put(f.getName(), SWCUtility.parse(f));
			}
			eu.mihosoft.vrl.system.VMessage.info("Computing density", "Total number of files for density computation: " + swcFiles.length);

		} catch (IOException e) {
			eu.mihosoft.vrl.system.VMessage.exception("File not found", e.toString());
		}

		/// density must respect new rescaled geometry and therefore fit in cuboid
		Density density = DensityUtil.computeDensity(cells, width, height, depth, choice);
		double dim = Collections.max(Arrays.asList(SWCUtility.getDimensions(cells).x, SWCUtility.getDimensions(cells).y, SWCUtility.getDimensions(cells).z));
		/* @todo the vta is way to big, since the geometry/density get's rescaled with the VisUtil.
		         we could however rescale the cube too, or we just omit the cube for rendering,
			since it isn't necessary in fact...
					
		*/
		
		VTriangleArray vta = new Cube(dim, dim, dim).toCSG().toVTriangleArray();
		// we could also fill the line graph geometry in the VTA array!!!
		return new Object[]{new DensityResult(density, vta), cells};
	}
}