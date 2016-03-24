/// package's name
package edu.gcsc.vrl.swcdensityvis.importer.XML;

/// imports
import edu.gcsc.vrl.densityvis.Density;
import edu.gcsc.vrl.swcdensityvis.data.Edge;
import edu.gcsc.vrl.swcdensityvis.importer.AbstractDensityComputationStrategyFactory;
import edu.gcsc.vrl.swcdensityvis.importer.DensityComputationContext;
import edu.gcsc.vrl.swcdensityvis.importer.DensityComputationStrategyFactoryProducer;
import edu.gcsc.vrl.swcdensityvis.importer.DensityData;
import edu.gcsc.vrl.swcdensityvis.importer.DensityVisualizable;
import edu.gcsc.vrl.swcdensityvis.util.MemoryUtil;
import eu.mihosoft.vrl.v3d.Shape3DArray;
import eu.mihosoft.vrl.v3d.VGeometry3D;
import eu.mihosoft.vrl.v3d.jcsg.Cylinder;
import eu.mihosoft.vrl.v3d.jcsg.Vector3d;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4d;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;

/**
 *
 * @author stephan
 */
public class XMLDensityVisualizerDiameterImpl implements DensityVisualizable, XMLDensityVisualizerImplementable {
	private final AbstractDensityComputationStrategyFactory strategyFactory = new DensityComputationStrategyFactoryProducer().getDefaultAbstractDensityComputationStrategyFactory(); /// edge factory 

	private DensityComputationContext context = new DensityComputationContext(strategyFactory.getDefaultComputationStrategy("XML")); /// get xml implementation of that strategy

	private final SAXBuilder saxBuilder = new SAXBuilder(XMLReaders.NONVALIDATING);
	private final HashMap<String, HashMap<String, Contour<Vector4d>>> contours = new HashMap<String, HashMap<String, Contour<Vector4d>>>();
	private final HashMap<String, HashMap<String, Tree<Vector4d>>> trees = new HashMap<String, HashMap<String, Tree<Vector4d>>>();
	private final Random r = new Random(System.currentTimeMillis());

	/// proxy members
	private Shape3DArray lineGraphGeometry;
	private Density density;
	private boolean isGeometryModified;
	private final ArrayList<File> inputFiles = new ArrayList<File>();
	private Color gColor = Color.WHITE;
	private double SF = 1;

	/**
	 *
	 * @param scalingFactor
	 */
	@Override
	public void setScalingFactor(double scalingFactor) {
		SF = scalingFactor;
	}

	/**
	 *
	 * @param files
	 */
	@Override
	public void setFiles(ArrayList<File> files) {
		this.inputFiles.addAll(files);
	}

	/**
	 * @brief only the first file of the list is parsed and processed
	 */
	@Override
	public void parse() {
		try {
			System.out.print("Building DOM structure...");
			Document document = saxBuilder.build(this.inputFiles.get(0));
			Element rootNode = document.getRootElement();
			System.out.println(" done!");
			System.out.println("root node: " + rootNode.toString());
			if (!rootNode.toString().equalsIgnoreCase("[Element: <mbf/>]")) {
				eu.mihosoft.vrl.system.VMessage.warning("ComputeDensity", "XML in wrong format, trying to auto-correct XML file now!");
				XMLFileUtil.fixXMLFile(this.inputFiles.get(0).getAbsolutePath());
				/// re-parse on success
				parse();
			} else {
				System.out.println("Processing Contours...");
				this.contours.put(this.inputFiles.get(0).getName(), process_contours(rootNode));
				System.out.println(" done!");

				/// TODO: here we add trees and contours but in fact we could have more cells not one cell
				/// therefore we work on the trees of the given cell, thus we normalized wrongly the density
				System.out.println("Processing Trees...");
				this.trees.put(this.inputFiles.get(0).getName(), process_trees(rootNode));
				System.out.println(" done!");

				this.inputFiles.remove(0);
				isGeometryModified = true;

				/// output trees
				for (Map.Entry<String, HashMap<String, Tree<Vector4d>>> entry : trees.entrySet()) {
					System.err.println("Input file: " + entry.getKey() + " has " + entry.getValue().size() + "trees");
				}
			
				/// output contours
				for (Map.Entry<String, HashMap<String, Contour<Vector4d>>> entry : contours.entrySet()) {
					System.err.println("Input file: " + entry.getKey() + " has " + entry.getValue().size() + "contours");
				}
			}
		} catch (IOException io) {
			System.out.println(io.getMessage());
		} catch (JDOMException jdomex) {
			System.out.println(jdomex.getMessage());
		}
	}

	/**
	 * @brief process_branches
	 */
	private void process_branches(List<Element> branches, Tree<Vector4d> t, Vector4d point_before_branch) {
		ArrayList<Edge<Vector4d>> edges = new ArrayList<Edge<Vector4d>>();

		for (Element branch : branches) {
			List<Element> points = branch.getChildren("point");
			if (points.size() >= 1) {
				edges.add(new Edge<Vector4d>(
					point_before_branch,
					new Vector4d(SF * Double.parseDouble(points.get(0).getAttributeValue("x")),
						SF * Double.parseDouble(points.get(0).getAttributeValue("y")),
						SF * Double.parseDouble(points.get(0).getAttributeValue("z")),
					        SF * Double.parseDouble(points.get(0).getAttributeValue("d")))));
			}

			for (int i = 0; i < points.size() - 1; i++) {
				edges.add(new Edge<Vector4d>(
					new Vector4d(SF * Double.parseDouble(points.get(i).getAttributeValue("x")),
						SF * Double.parseDouble(points.get(i).getAttributeValue("y")),
						SF * Double.parseDouble(points.get(i).getAttributeValue("z")),
					        SF * Double.parseDouble(points.get(i).getAttributeValue("d"))),
					new Vector4d(SF * Double.parseDouble(points.get(i + 1).getAttributeValue("x")),
						SF * Double.parseDouble(points.get(i + 1).getAttributeValue("y")),
						SF * Double.parseDouble(points.get(i + 1).getAttributeValue("z")),
					        SF * Double.parseDouble(points.get(i + 1).getAttributeValue("d")))));
			}

			// append edges
			ArrayList<Edge<Vector4d>> edges_new = new ArrayList<Edge<Vector4d>>();
			if (!(t.getEdges() == null)) {
				edges_new.addAll(t.getEdges());
			}
			if (!edges.isEmpty()) {
				edges_new.addAll(edges);
			}
			t.setEdges(edges_new);

			Vector4d point_before_branch_new;
			/// if no point before next branch, then we must connect that point with a point after the next branch
			if (points.isEmpty()) {
				point_before_branch_new = point_before_branch;
				/// if only one point is present, then this is the point we create an edge to the point in the next branch
			} else if (points.size() == 1) {
				point_before_branch_new = new Vector4d(SF * Double.parseDouble(points.get(0).getAttributeValue("x")),
					SF * Double.parseDouble(points.get(0).getAttributeValue("y")),
					SF * Double.parseDouble(points.get(0).getAttributeValue("z")),
					SF * Double.parseDouble(points.get(0).getAttributeValue("d")));
			} else {
				/// else the point which defines an edge with the next branch's point is the last point from the current branch
				point_before_branch_new = edges.get(edges.size() - 1).getTo();
			}

			if (!branch.getChildren("branch").isEmpty()) {
				System.err.println("Branch does contain a branch!");
				process_branches(branch.getChildren("branch"), t, point_before_branch_new);
			} else {
				System.err.println("Branch does not contain a branch!");
			}
		}
	}


	/**
	 * @brief process one tree
	 * @param node
	 * @return 
	 */
	private Tree<Vector4d> process_tree(Element node) {
			ArrayList< Edge< Vector4d>> edges = new ArrayList<Edge<Vector4d>>();
			Tree<Vector4d> t = new Tree<Vector4d>();
			String name = node.getAttributeValue("type");
			t.setType(name);
			String color = node.getAttributeValue("color");
			t.setColor(Color.decode(color));
			t.setType(node.getAttributeValue("type"));
			String leaf = node.getAttributeValue("leaf");
			t.setLeaf(leaf);
			List<Element> points = node.getChildren("point");
			for (int i = 0; i < points.size() - 1; i++) {
				edges.add(new Edge<Vector4d>(
					new Vector4d(SF * Double.parseDouble(points.get(i).getAttributeValue("x")),
						SF * Double.parseDouble(points.get(i).getAttributeValue("y")),
						SF * Double.parseDouble(points.get(i).getAttributeValue("z")),
						SF * Double.parseDouble(points.get(i).getAttributeValue("d"))),
					new Vector4d(SF * Double.parseDouble(points.get(i + 1).getAttributeValue("x")),
						SF * Double.parseDouble(points.get(i + 1).getAttributeValue("y")),
						SF * Double.parseDouble(points.get(i + 1).getAttributeValue("z")),
						SF * Double.parseDouble(points.get(i + 1).getAttributeValue("d")))));
			}

			// append edges
			ArrayList<Edge<Vector4d>> edges_new = new ArrayList<Edge<Vector4d>>();
			if (!(t.getEdges() == null)) {
				edges_new.addAll(t.getEdges());
			}
			if (!edges.isEmpty()) {
				edges_new.addAll(edges);
			}
			t.setEdges(edges_new);

			/// at least one point must be present before branching obvious
			Vector4d point_before_branch = edges.get(edges.size() - 1).getTo();
			if (!node.getChildren("branch").isEmpty()) {
				System.err.println("Tree has branches!");
				process_branches(node.getChildren("branch"), t, point_before_branch);
			} else {
				System.err.println("Tree has no branches!");
			}
			System.err.println("Name: " + name);
			
			/*for (Edge<Vector4d> edge : trees.get(name).getEdges()) {
				System.err.println("from: " + edge.getFrom() + " to: " + edge.getTo());
			}*/
		return t;
	}

	/**
	 * @brief process trees
	 * @todo respectively note: it seems to be the case that trees aren't nested!
	 */
	private HashMap<String, Tree<Vector4d>> process_trees(Element rootNode) {
		HashMap<String, Tree<Vector4d>> trees_ = new HashMap<String, Tree<Vector4d>>();

		int index = 0;
		for (Element node : rootNode.getChildren("tree")) {
			Tree<Vector4d> entry = process_tree(node);
			trees_.put(node.getAttributeValue("type") + " #" + index, entry) ;
			index++;
		}
		
		for (Map.Entry<String, Tree<Vector4d>> entry : trees_.entrySet()) {
			System.err.println("name of tree: " + entry.getKey());
		}
		
		return trees_;
	}

	/**
	 * @brief process contours
	 * @todo respectively note: contours seem not to be allowed to be nested!
	 * @param rootNode
	 */
	private HashMap<String, Contour<Vector4d>> process_contours(Element rootNode) {
		HashMap<String, Contour<Vector4d>> contours_ = new HashMap<String, Contour<Vector4d>>();
		int index = 0;
		for (Element node : rootNode.getChildren("contour")) {
			Contour<Vector4d> c = new Contour<Vector4d>();
			String color = node.getAttributeValue("color");
			c.setColor(Color.decode(color));
			String contourName = node.getAttributeValue("name");
			c.setName(contourName);
			String shape = node.getAttributeValue("shape");
			c.setName(shape);
			String closed = node.getAttributeValue("closed");
			c.setClosed(Boolean.valueOf(closed));
			
			ArrayList<Vector4d> points = new ArrayList<Vector4d>();
			for (Element point : node.getChildren("point")) {
				points.add(new Vector4d(SF * Double.parseDouble(point.getAttributeValue("x")),
					SF * Double.parseDouble(point.getAttributeValue("y")),
					SF * Double.parseDouble(point.getAttributeValue("z")),
					SF * Double.parseDouble(point.getAttributeValue("d"))));

			}
			c.setPoints(points);
			contours_.put(contourName + " # " + index, c);
			index++;
			
			for (Vector4d vec : points) {
				System.out.println(vec);
			}
		}
		return contours_;
	}

	/**
	 * @brief all files of the list are parsed and processed
	 */
	@Override
	public void parseStack() {
		while (!inputFiles.isEmpty()) {
			parse();
		}
	}

	/**
	 *  @brief delegate to strategy 
	 *  @return
	 */
	@Override
	public Vector3f getDimension() {
		return (Vector3f) this.context.getDensityComputationStrategy().getDimension();
	}


	/**
	 * 
	 * @return 
	 */
	@Override
	public Density computeDensity() {
		if (density == null || isGeometryModified) {
			/// NOTE: local data must be also something like this:
			/// HashMap<String, HashMap<String, ArrayList<Edge<Vector3f>>>> filename_local_data;
			HashMap<String, ArrayList<Edge<Vector3f>>> local_data = new HashMap<String, ArrayList<Edge<Vector3f>>>();
			for (Map.Entry<String, HashMap<String, Tree<Vector4d>>> cell : trees.entrySet()) {
				ArrayList<Edge<Vector3f>> final_data = new ArrayList<Edge<Vector3f>>();
				for (Map.Entry<String, Tree<Vector4d>> tree : cell.getValue().entrySet()) {
					System.err.println("Number of edges (computeDensity): " + tree.getValue().getEdges().size());
					ArrayList<Edge<Vector3f>> points = new ArrayList<Edge<Vector3f>>();
					for (Edge<Vector4d> vec : tree.getValue().getEdges()) {
						Vector4d from = vec.getFrom();
						Vector4d to = vec.getTo();
						points.add(new Edge<Vector3f>(
							new Vector3f( (float) from.x, (float) from.y, (float) from.z),
							new Vector3f( (float) to.x, (float) to.y, (float) to.z)));
					}
					final_data.addAll(points);
					/// local_data.put(tree.getKey(), points);
				}
				local_data.put(cell.getKey(), final_data);
				
			}
			for (Map.Entry<String, HashMap<String, Tree<Vector4d>>> cell : trees.entrySet()) {
				System.err.println("cell name/file name: " + cell.getKey().toString());
			}
			XMLDensityData data = new XMLDensityData(local_data);
			this.context.setDensityData(data);
			this.density = context.executeDensityComputation();
		}

		return this.density;
	}

	/**
	 * 
	 * @param color 
	 */
	@Override
	public void setLineGraphColor(Color color) {
		this.gColor = color;
	}

	/**
	 * 
	 * @return 
	 */
	@Override
	@SuppressWarnings("ReturnOfCollectionOrArrayField")
	public Shape3DArray calculateGeometry() {
		if (this.lineGraphGeometry == null || isGeometryModified) {
			this.lineGraphGeometry = new Shape3DArray();
			/// visualize trees!
			for (HashMap<String, Tree<Vector4d>> ts : trees.values()) {
				for (Tree<Vector4d> t : ts.values()) {
					/// if more than one tree, assign random color for this tree!
					/**
					 * @todo probably we want a fixed color scheme for the compartments
					 * @todo now we set the colors by xml file!
					 */
					/*if (ts.values().size() != 1) {
						gColor = new Color(r.nextInt(256), r.nextInt(256), r.nextInt(256));
					}*/
					gColor = t.getColor();
					
					System.err.println("Edges: " + t.getEdges().size());
					for (Edge<Vector4d> e : t.getEdges()) {
						//System.err.println("Edge: " + e);
						MemoryUtil.printHeapMemoryUsage();			
						Cylinder cyl = new Cylinder(new Vector3d(e.getFrom().x, e.getFrom().y, e.getFrom().z), new Vector3d(e.getTo().x, e.getTo().y, e.getTo().z), e.getFrom().w, e.getTo().w, 3);
						
						this.lineGraphGeometry.addAll(new VGeometry3D(cyl.toCSG().toVTriangleArray(), new Color(gColor.getRed(), gColor.getGreen(), gColor.getBlue()),null, 1F, false, false, false).generateShape3DArray());
						MemoryUtil.printHeapMemoryUsage();
					}
					
					System.err.println("next tree!");
				}
			}
			
			System.err.println("trees done!");
			
			/// visualize contours!
			/**
			 * @todo
			 */
			for (HashMap<String, Contour<Vector4d>> cs : contours.values()) {
				for (Contour<Vector4d> con : cs.values()) {
					gColor = con.getColor();
					ArrayList<Vector4d> points = con.getPoints();
					/**
					 * @todo refactor to use edge instead of points
					 */
					MemoryUtil.printHeapMemoryUsage();
					for (int i = 0; i < points.size() - 1; i++) {
						Cylinder cyl = new Cylinder(new Vector3d(points.get(i).x, points.get(i).y, points.get(i).z), new Vector3d(points.get(i+1).x, points.get(i+1).y, points.get(i+1).z), points.get(i).w, points.get(i+1).w, 3);
						this.lineGraphGeometry.addAll(new VGeometry3D(cyl.toCSG().toVTriangleArray(), new Color(gColor.getRed(), gColor.getGreen(), gColor.getBlue()),null, 1F, false, false, false).generateShape3DArray());
						
					}
					MemoryUtil.printHeapMemoryUsage();
					
				}
				
			}
			
		}

		System.err.println("contours done!");
		
		isGeometryModified = false;
		return this.lineGraphGeometry;
	}

	/**
	 * 
	 * @param densityComputationContext 
	 */
	@Override
	public void setContext(DensityComputationContext densityComputationContext) {
		context = densityComputationContext;
	}

	/**
	 * 
	 * @param data 
	 */
	@Override
	public void setDensityData(DensityData data) {
		this.context.setDensityData(data);
	}

	/**
	 * 
	 * @return 
	 */
	@Override
	public Object getCenter() {
		return this.context.getDensityComputationStrategy().getCenter();
	}

}
