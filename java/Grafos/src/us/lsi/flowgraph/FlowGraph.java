package us.lsi.flowgraph;

import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.io.DOTExporter;
import org.jgrapht.io.IntegerComponentNameProvider;
import us.lsi.common.Files2;
import us.lsi.common.Preconditions;
import us.lsi.common.Strings2;
import us.lsi.graphcolors.GraphColors;
import us.lsi.graphs.GraphsReader;


/**
 * @author Miguel Toro
 * 
 * Un grafo simple dirigido y sin peso. La informaci�n de la red est�
 * guardada en los v�rtices y las aristas que son de los tipos 
 * FlowVertex y FlowEdge.
 *
 */


public class FlowGraph extends SimpleDirectedGraph<FlowVertex, FlowEdge> {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Un v�rtice de una red de Flujo puede ser una Fuente, un Sumidero o
	 * un v�rtice intermedio
	 *
	 */
	public enum TipoDeOptimizacion{Max,Min}	
	
	public static Boolean allInteger = false;

	public static FlowGraph create() {
		return new FlowGraph(FlowEdge.class);
	}

	private FlowGraph(Class<? extends FlowEdge> arg0) {
		super(arg0);
	}
	
	private TipoDeOptimizacion tipo = TipoDeOptimizacion.Max;
	private String constraints = null;

	public static FlowGraph newGraph(String file, TipoDeOptimizacion tipo) {
		FlowGraph r = GraphsReader.<FlowVertex,FlowEdge,FlowGraph>
		    newGraph(file, 
				FlowVertex::create, 
				FlowEdge::create, 
				FlowGraph::create);
		Preconditions.checkArgument(check(r),"Grafo de flujo mal formado");
		r.tipo = tipo;
		return r;		
	}
	
	private static boolean check(FlowGraph fg){
		boolean r = true;
		for(FlowVertex v: fg.vertexSet()){
			if(v.isSource()){
				r = fg.incomingEdgesOf(v).isEmpty();
			}
			if(!r) break;
			if(v.isSink()){
				r = fg.outgoingEdgesOf(v).isEmpty();
			}
			if(!r) break;
		}
		return r;
	}

	public TipoDeOptimizacion getTipo() {
		return tipo;
	}

	private String kirchoff(FlowVertex v) {
		String r = "";
		if(v.isSource()) {
			r = v.getVariable()+" = "+
				 Strings2.format(this.outgoingEdgesOf(v), x->x.getVariable(), "+")+";\n";
		} else if(v.isSink()){
			r = Strings2.format(this.incomingEdgesOf(v), x->x.getVariable(), "+")+" = "+v.getVariable()+";\n";			 
		} else {
			String in = Strings2.format(this.incomingEdgesOf(v), x->x.getVariable(), "+");
			r = v.getVariable() + " = " + in + ";\n";
			r = r + in +" = "+ Strings2.format(this.outgoingEdgesOf(v), x->x.getVariable(), "+")+";\n";
		}
		return r;
	}
	
	public String getConstraints() {
		if (this.constraints == null) {
			String goal = tipo.equals(TipoDeOptimizacion.Min) ? "min: " : "max: ";
			goal = goal + Strings2.format(this.vertexSet(), v -> v.toObjective(), "");
			goal = goal + Strings2.format(this.edgeSet(), e -> e.toObjective(), "");
			goal = goal + ";\n";
			goal = goal + Strings2.format(this.vertexSet(), v -> v.toConstraints(), "");
			goal = goal + Strings2.format(this.edgeSet(), e -> e.toConstraints(), "");
			goal = goal + Strings2.format(this.vertexSet(), v -> this.kirchoff(v), "");
			if (FlowGraph.allInteger) {
				goal = goal + "int " + Strings2.format(this.vertexSet(), v -> v.getVariable(), ",");
				goal = goal + ",";
				goal = goal + Strings2.format(this.edgeSet(), v -> v.getVariable(), ",");
				goal = goal + ";\n";
			}
			this.constraints = goal;
		}
		return this.constraints;
	}
	
	public void exportToDot(String file) {
		DOTExporter<FlowVertex, FlowEdge> de = 
				new DOTExporter<FlowVertex, FlowEdge>(
					new IntegerComponentNameProvider<>(),
					v->v.getName(),
					e->e.getName(),
					v->GraphColors.getFilledColor(v.getColor()),
					null);
		de.exportGraph(this, Files2.getWriter(file));
	}
	
	private String vertexFormat(FlowVertex v) {
		return String.format("%s,%.1f,%s,%.1f",
				v.getVariable(),
				v.getMin(),
				v.getMax()<Double.MAX_VALUE?String.format("%.1f",v.getMax()):"_",
				v.getCost());
	}
	
	private String edgeFormat(FlowEdge e) {
		return String.format("%s,%.1f,%s,%.1f",
				e.getVariable(),
				e.getMin(),
				e.getMax()<Double.MAX_VALUE?String.format("%.1f",e.getMax()):"_",
				e.getCost());
	}
	
	public void exportToDotVariables(String file) {
		DOTExporter<FlowVertex, FlowEdge> de = 
				new DOTExporter<FlowVertex, FlowEdge>(
					new IntegerComponentNameProvider<>(),
					v->vertexFormat(v),
					e->edgeFormat(e));
		de.exportGraph(this, Files2.getWriter(file));
	}

}
