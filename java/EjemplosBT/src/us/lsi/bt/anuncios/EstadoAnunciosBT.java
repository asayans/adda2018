package us.lsi.bt.anuncios;



import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import us.lsi.anuncios.datos.Anuncio;
import us.lsi.anuncios.datos.DatosAnuncios;
import us.lsi.anuncios.datos.ListaDeAnunciosAEmitir;
import us.lsi.bt.EstadoBT;
import us.lsi.common.Lists2;
import us.lsi.common.Sets2;
import us.lsi.common.Tuple2;



public class EstadoAnunciosBT implements 
    EstadoBT<ListaDeAnunciosAEmitir, Integer, EstadoAnunciosBT> {
	
	public static Double mejorValorObtenido = Double.MIN_VALUE;	
	public static ListaDeAnunciosAEmitir mejorSolucionObtenida = null;
	
	public static EstadoAnunciosBT create() {
		return new EstadoAnunciosBT();
	}

	public static EstadoAnunciosBT create(List<Integer> lista) {
		return new EstadoAnunciosBT(lista);
	}

	private ListaDeAnunciosAEmitir lista;
	private List<Integer> listaDeEnteros;

	private EstadoAnunciosBT() {
		super();
		this.listaDeEnteros = Lists2.newList();
		this.lista = ListaDeAnunciosAEmitir.create(listaDeEnteros);
	}
	
	private EstadoAnunciosBT(List<Integer> lista) {
		super();
		this.listaDeEnteros = Lists2.newList(lista);
		this.lista = ListaDeAnunciosAEmitir.create(lista);
	}

	@Override
	public us.lsi.bt.EstadoBT.Tipo getTipo() {
		return Tipo.Max;
	}

	@Override
	public EstadoAnunciosBT getEstadoInicial() {
		return EstadoAnunciosBT.create();
	}
	
	@Override
	public EstadoAnunciosBT avanza(Integer a) {
		this.listaDeEnteros.add(a);
		this.lista = ListaDeAnunciosAEmitir.create(this.listaDeEnteros);
		return this;
	}

	@Override
	public EstadoAnunciosBT retrocede(Integer a) {
		this.listaDeEnteros.remove(a);
		this.lista = ListaDeAnunciosAEmitir.create(this.listaDeEnteros);
		return this;
	}

	@Override
	public int size() {
		return lista.getAnunciosDisponibles().size();
	}

	@Override
	public boolean esCasoBase() {
		return lista.getAnunciosDisponibles().isEmpty();
	}

	@Override
	public List<Integer> getAlternativas() {
		return lista.getAnunciosDisponibles().stream().collect(Collectors.toList());
	}

	@Override
	public ListaDeAnunciosAEmitir getSolucion() {
		return lista;
	}	

	@Override
	public Double getObjetivoEstimado(Integer a) {
		return (lista.getValor()+ cota(a));
	}

	private boolean comprueba(Integer e, Set<Integer> s){
		boolean r = true;
		for(Tuple2<Integer, Integer> p: DatosAnuncios.restricciones){
			if(s.contains(p.v1) && p.v2 == e){
				r = false;
				break;
			}
		}
		return r;
	}
	
	public Double cota(Integer a) {
		Integer tr = lista.getTiempoRestante();
		Integer te = lista.getTiempoConsumido();
		Double r = 0.;
		Anuncio an = DatosAnuncios.getAnuncio(a);
		Integer dr = an.getDuracion();		
		r = r + an.getPrecio(te+1);
		tr = tr - dr;
		te = te + dr;
		Set<Integer> s = Sets2.newSet(lista.getAnunciosDecididosParaEmitirSet());
		s.add(a);
		for(Integer e : lista.getAnunciosDisponibles()){
			if(e == a) continue;
			if(!comprueba(e,s)) continue;
			an = DatosAnuncios.getAnuncio(e);
			dr = an.getDuracion();
			if(tr ==0) break;
			if(dr < tr){
				r = r + an.getPrecio(te+1);
				tr = tr - dr;
				te = te + dr;
				s.add(e);
			} else {
				r = r + an.getPrecio(te+1)*(dr/tr);
				break;
			}
		}
		return r;
	}

	

	

	
}
