package com.shelter.ui.vistas;

import com.shelter.controller.ShelterController;
import com.shelter.model.Albergue;
import com.shelter.model.Alerta;
import com.shelter.model.Damnificado;
import com.shelter.model.NivelSemaforo;
import com.shelter.model.Recurso;
import com.shelter.model.TipoRecurso;
import com.shelter.service.FiltroAlbergue;
import com.shelter.ui.componentes.BarraNivel;
import com.shelter.ui.componentes.BotonModerno;
import com.shelter.ui.componentes.FilaAlerta;
import com.shelter.ui.componentes.Formatos;
import com.shelter.ui.componentes.Formularios;
import com.shelter.ui.componentes.Iconos;
import com.shelter.ui.componentes.Renderizadores;
import com.shelter.ui.componentes.SemaforoComponente;
import com.shelter.ui.componentes.Tarjeta;
import com.shelter.ui.componentes.TarjetaRecurso;
import com.shelter.ui.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Tablero: primero QUÉ HACER, después cómo está la red.
 *
 * Distill: el semáforo pasó de ocupar un cuarto de la pantalla a ser un indicador compacto junto
 * al nombre del albergue, y el estado se dice una sola vez por zona (en la tabla, la columna
 * Estado; en el albergue elegido, el semáforo con su etiqueta). El espacio liberado es para las
 * alertas, en tamaño de lectura y cada una con la acción que la resuelve.
 */
public class PanelTablero extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient ShelterController controller;
    private final transient Contexto contexto;

    // Albergue elegido
    private final SemaforoComponente semaforo = new SemaforoComponente();
    private final JLabel lblNombre = new JLabel("Sin albergue");
    private final JLabel lblUbicacion = new JLabel(" ");
    private final JLabel lblEstado = new JLabel(" ");
    private final BarraNivel barraOcupacion = new BarraNivel();
    private final JLabel lblOcupacion = new JLabel(" ");
    private final FilaAlerta.Pila pilaAlertas = new FilaAlerta.Pila();

    // Red de albergues + filtros
    private final ModeloAlbergues modeloAlbergues = new ModeloAlbergues();
    private final JTable tablaAlbergues = Renderizadores.tabla(modeloAlbergues,
            () -> "Ningún albergue coincide con los filtros. Pulsa Limpiar para ver toda la red, "
                    + "o registra uno con Nuevo albergue.");
    private final JTextField txtDistrito = Formularios.campo("");
    private final JSpinner spnCupos = new JSpinner(new SpinnerNumberModel(0, 0, 100000, 1));
    private final JComboBox<Object> cmbNivel = new JComboBox<>();
    private final JComboBox<Object> cmbSinStock = new JComboBox<>();

    // Detalle
    private final TarjetaRecurso[] mosaicos = new TarjetaRecurso[TipoRecurso.values().length];
    private final DefaultListModel<Damnificado> modeloDamnificados = new DefaultListModel<>();
    private final ModeloLotes modeloLotes = new ModeloLotes();

    private boolean refrescando;

    public PanelTablero(ShelterController controller, Contexto contexto) {
        super(new BorderLayout(16, 0));
        this.controller = controller;
        this.contexto = contexto;
        setOpaque(false);
        add(construirAlbergueElegido(), BorderLayout.WEST);
        add(construirCentro(), BorderLayout.CENTER);
    }

    // =========================================================
    // COLUMNA IZQUIERDA: el albergue elegido y qué hacer
    // =========================================================

    private Tarjeta construirAlbergueElegido() {
        lblNombre.setFont(Tema.TITULO);
        lblNombre.setForeground(Tema.TEXTO);
        lblUbicacion.setFont(Tema.TEXTO_NORMAL);
        lblUbicacion.setForeground(Tema.TEXTO_SUAVE);
        lblEstado.setFont(Tema.SUBTITULO);
        lblOcupacion.setFont(Tema.TEXTO_NORMAL);
        lblOcupacion.setForeground(Tema.TEXTO_SUAVE);
        barraOcupacion.setMostrarTexto(false);
        barraOcupacion.setPreferredSize(new Dimension(200, 14));
        barraOcupacion.setMaximumSize(new Dimension(Integer.MAX_VALUE, 14));

        JPanel datos = new JPanel();
        datos.setOpaque(false);
        datos.setLayout(new BoxLayout(datos, BoxLayout.Y_AXIS));
        for (javax.swing.JComponent c : new javax.swing.JComponent[]{lblNombre, lblUbicacion, lblEstado, barraOcupacion, lblOcupacion}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        datos.add(lblNombre);
        datos.add(Box.createVerticalStrut(2));
        datos.add(lblUbicacion);
        datos.add(Box.createVerticalStrut(12));
        datos.add(lblEstado);
        datos.add(Box.createVerticalStrut(12));
        datos.add(barraOcupacion);
        datos.add(Box.createVerticalStrut(6));
        datos.add(lblOcupacion);

        semaforo.setPreferredSize(new Dimension(64, 168));
        JPanel cabecera = new JPanel(new BorderLayout(14, 0));
        cabecera.setOpaque(false);
        cabecera.add(semaforo, BorderLayout.WEST);
        cabecera.add(datos, BorderLayout.CENTER);

        JLabel queHacer = new JLabel("Qué hacer");
        queHacer.setFont(Tema.SUBTITULO);
        queHacer.setForeground(Tema.TEXTO);
        queHacer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDE_SUAVE),
                BorderFactory.createEmptyBorder(14, 0, 2, 0)));
        JScrollPane scrollAlertas = Tema.scroll(pilaAlertas);
        scrollAlertas.setUI(new javax.swing.plaf.basic.BasicScrollPaneUI()); // sin el fondo de Nimbus
        scrollAlertas.setBorder(BorderFactory.createEmptyBorder());
        scrollAlertas.setOpaque(false);
        scrollAlertas.getViewport().setOpaque(false);

        JPanel cuerpo = new JPanel(new BorderLayout(0, 14));
        cuerpo.setOpaque(false);
        cuerpo.add(cabecera, BorderLayout.NORTH);
        JPanel alertas = new JPanel(new BorderLayout(0, 4));
        alertas.setOpaque(false);
        alertas.add(queHacer, BorderLayout.NORTH);
        alertas.add(scrollAlertas, BorderLayout.CENTER);
        cuerpo.add(alertas, BorderLayout.CENTER);

        Tarjeta tarjeta = new Tarjeta("Albergue seleccionado");
        tarjeta.contenido(cuerpo);
        tarjeta.setPreferredSize(new Dimension(400, 100));
        return tarjeta;
    }

    // =========================================================
    // CENTRO: la red, los recursos y las personas
    // =========================================================

    private JPanel construirCentro() {
        JPanel centro = new JPanel(new GridBagLayout());
        centro.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;

        c.gridy = 0;
        c.weighty = 0.5;
        c.insets = new Insets(0, 0, 12, 0);
        centro.add(construirTablaRed(), c);

        c.gridy = 1;
        c.weighty = 0;
        // Una sola fila sobria: cinco columnas de texto separadas por líneas finas, dentro de una tarjeta
        JPanel filaMosaicos = new JPanel(new GridLayout(1, mosaicos.length, 0, 0));
        filaMosaicos.setOpaque(false);
        for (TipoRecurso tipo : TipoRecurso.values()) {
            mosaicos[tipo.ordinal()] = new TarjetaRecurso(tipo);
            mosaicos[tipo.ordinal()].setSeparador(tipo.ordinal() > 0);
            filaMosaicos.add(mosaicos[tipo.ordinal()]);
        }
        Tarjeta tRecursos = new Tarjeta(null);
        tRecursos.setBorder(BorderFactory.createEmptyBorder(14, 2, 14, 8));
        tRecursos.setToolTipText("Stock vigente del albergue seleccionado");
        c.insets = new Insets(0, 0, 12, 0);
        centro.add(tRecursos.contenido(filaMosaicos), c);

        c.gridy = 2;
        c.weighty = 0.5;
        c.insets = new Insets(0, 0, 0, 0);
        centro.add(construirDetalle(), c);
        return centro;
    }

    private Tarjeta construirTablaRed() {
        Tarjeta tarjeta = new Tarjeta("Red de albergues");
        tarjeta.setToolTipText("RF-05 panel semáforo · RF-07 filtros");

        BotonModerno btnNuevo = new BotonModerno("Nuevo albergue");
        btnNuevo.setIcon(Iconos.de(Iconos.Figura.MAS, 14, Tema.TINTA_ACCION));
        btnNuevo.addActionListener(e -> dialogoNuevoAlbergue());
        BotonModerno btnSphere = new BotonModerno("Validar Sphere", BotonModerno.Estilo.SECUNDARIO);
        btnSphere.setIcon(Iconos.de(Iconos.Figura.ESCUDO, 14, Tema.ACENTO));
        btnSphere.setToolTipText("RF-06: revisa agua y espacio vital en toda la red");
        btnSphere.addActionListener(e -> reporteSphere());
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.setOpaque(false);
        acciones.add(btnSphere);
        acciones.add(btnNuevo);
        tarjeta.accionesTitulo(acciones);

        // Filtros (RF-07): se aplican en vivo
        cmbNivel.setRenderer(new Renderizadores.TextoCombo());
        cmbSinStock.setRenderer(new Renderizadores.TextoCombo());
        cmbNivel.addItem("Todos");
        for (NivelSemaforo n : NivelSemaforo.values()) {
            cmbNivel.addItem(n);
        }
        cmbSinStock.addItem("Cualquiera");
        for (TipoRecurso t : TipoRecurso.values()) {
            cmbSinStock.addItem(t);
        }
        txtDistrito.setColumns(8);
        txtDistrito.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refrescar(); }
            @Override public void removeUpdate(DocumentEvent e) { refrescar(); }
            @Override public void changedUpdate(DocumentEvent e) { refrescar(); }
        });
        spnCupos.addChangeListener(e -> refrescar());
        cmbNivel.addActionListener(e -> refrescar());
        cmbSinStock.addActionListener(e -> refrescar());
        BotonModerno btnLimpiar = new BotonModerno("Limpiar", BotonModerno.Estilo.SECUNDARIO);
        btnLimpiar.addActionListener(e -> {
            txtDistrito.setText("");
            spnCupos.setValue(0);
            cmbNivel.setSelectedIndex(0);
            cmbSinStock.setSelectedIndex(0);
        });

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtros.setOpaque(false);
        filtros.add(Formularios.etiqueta("Distrito"));
        filtros.add(txtDistrito);
        filtros.add(Formularios.etiqueta("Cupos mín."));
        filtros.add(spnCupos);
        filtros.add(Formularios.etiqueta("Estado"));
        filtros.add(cmbNivel);
        filtros.add(Formularios.etiqueta("Sin stock de"));
        filtros.add(cmbSinStock);
        filtros.add(btnLimpiar);

        Tema.estilizar(tablaAlbergues);
        tablaAlbergues.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaAlbergues.setDefaultRenderer(Object.class, new Renderizadores.Texto());
        tablaAlbergues.getColumnModel().getColumn(0).setCellRenderer(new Renderizadores.Texto(true));
        tablaAlbergues.getColumnModel().getColumn(5).setCellRenderer(new Renderizadores.OcupacionCelda());
        tablaAlbergues.getColumnModel().getColumn(7).setCellRenderer(new Renderizadores.SemaforoCelda());
        int[] anchos = {55, 160, 110, 110, 70, 140, 110, 110};
        for (int i = 0; i < anchos.length; i++) {
            tablaAlbergues.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        tablaAlbergues.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !refrescando) {
                Albergue a = albergueSeleccionado();
                if (a != null) {
                    contexto.setIdAlbergue(a.getId()); // las demás secciones abrirán este albergue
                }
                mostrarDetalle();
            }
        });

        JPanel contenido = new JPanel(new BorderLayout(0, 10));
        contenido.setOpaque(false);
        contenido.add(filtros, BorderLayout.NORTH);
        contenido.add(Tema.scroll(tablaAlbergues), BorderLayout.CENTER);
        return tarjeta.contenido(contenido);
    }

    private JPanel construirDetalle() {
        JPanel fila = new JPanel(new GridLayout(1, 2, 12, 0));
        fila.setOpaque(false);

        JList<Damnificado> listaDamnificados = Renderizadores.lista(modeloDamnificados,
                new Renderizadores.DamnificadoLista(false),
                () -> albergueSeleccionado() == null ? ""
                        : "Nadie alojado todavía. Registra un ingreso en la sección Damnificados.");
        fila.add(new Tarjeta("Personas alojadas").contenido(Tema.scroll(listaDamnificados)));

        JTable tablaLotes = Renderizadores.tabla(modeloLotes,
                () -> albergueSeleccionado() == null ? "" : "Sin lotes. Registra recursos en la sección Recursos.");
        Tema.estilizar(tablaLotes);
        tablaLotes.setDefaultRenderer(Object.class, new Renderizadores.Texto());
        tablaLotes.getColumnModel().getColumn(0).setCellRenderer(new Renderizadores.LoteCelda());
        tablaLotes.getColumnModel().getColumn(0).setPreferredWidth(170);
        fila.add(new Tarjeta("Inventario").contenido(Tema.scroll(tablaLotes)));
        return fila;
    }

    // =========================================================
    // REFRESCO
    // =========================================================

    public void refrescar() {
        String seleccionado = contexto.getIdAlbergue();
        if (albergueSeleccionado() != null) {
            seleccionado = albergueSeleccionado().getId();
        }
        refrescando = true;
        try {
            modeloAlbergues.setDatos(controller.filtrar(filtroActual()));
            seleccionarFila(seleccionado);
        } finally {
            refrescando = false;
        }
        mostrarDetalle();
    }

    /** Al volver a esta sección: mostrar el albergue en foco (pudo cambiar en otra sección). */
    public void alMostrar() {
        refrescando = true;
        try {
            seleccionarFila(contexto.getIdAlbergue());
        } finally {
            refrescando = false;
        }
        mostrarDetalle();
    }

    private void seleccionarFila(String id) {
        int fila = -1;
        for (int i = 0; i < modeloAlbergues.getRowCount(); i++) {
            if (modeloAlbergues.get(i).getId().equalsIgnoreCase(String.valueOf(id))) {
                fila = i;
            }
        }
        if (fila < 0 && modeloAlbergues.getRowCount() > 0) {
            fila = 0;
        }
        if (fila >= 0) {
            tablaAlbergues.setRowSelectionInterval(fila, fila);
            tablaAlbergues.scrollRectToVisible(tablaAlbergues.getCellRect(fila, 0, true));
            if (contexto.getIdAlbergue() == null) {
                contexto.setIdAlbergue(modeloAlbergues.get(fila).getId());
            }
        }
    }

    private FiltroAlbergue filtroActual() {
        int cupos = (Integer) spnCupos.getValue();
        Object nivel = cmbNivel.getSelectedItem();
        Object sinStock = cmbSinStock.getSelectedItem();
        return new FiltroAlbergue(
                cupos > 0 ? cupos : null,
                txtDistrito.getText().trim(),
                nivel instanceof NivelSemaforo n ? n : null,
                sinStock instanceof TipoRecurso t ? t : null);
    }

    private Albergue albergueSeleccionado() {
        int fila = tablaAlbergues.getSelectedRow();
        return fila >= 0 && fila < modeloAlbergues.getRowCount() ? modeloAlbergues.get(fila) : null;
    }

    private void mostrarDetalle() {
        Albergue a = albergueSeleccionado();
        modeloDamnificados.clear();
        pilaAlertas.removeAll();
        if (a == null) {
            lblNombre.setText("Sin albergue");
            lblUbicacion.setText("Ningún albergue coincide con los filtros");
            lblEstado.setText(" ");
            barraOcupacion.setPorcentaje(0, null);
            lblOcupacion.setText(" ");
            modeloLotes.setDatos(List.of());
            for (TarjetaRecurso m : mosaicos) {
                m.setDatos("—", "", false);
            }
            pilaAlertas.revalidate();
            pilaAlertas.repaint();
            return;
        }

        NivelSemaforo nivel = a.getEstadoSemaforo();
        List<Alerta> alertas = new ArrayList<>(controller.alertas(a));
        // Lo más grave primero: el encargado lee de arriba hacia abajo
        alertas.sort((x, y) -> y.gravedad().compareTo(x.gravedad()));

        semaforo.setNivel(nivel);
        lblNombre.setText(a.getNombre());
        lblUbicacion.setText(a.getId() + " · " + a.getDistrito() + " · " + a.getTipoInfraestructura());
        lblEstado.setForeground(Tema.mezclar(Tema.color(nivel), Color.WHITE, 0.15));
        lblEstado.setText("Estado " + nivel.getEtiqueta().toLowerCase() + " · "
                + (alertas.isEmpty() ? "sin alertas" : alertas.size() + (alertas.size() == 1 ? " alerta" : " alertas")));
        barraOcupacion.setPorcentaje(a.getPorcentajeOcupacion(), null);
        lblOcupacion.setText(a.getOcupacionActual() + " de " + a.getCapacidadMaxima() + " personas · "
                + a.getCuposDisponibles() + (a.getCuposDisponibles() == 1 ? " cupo libre" : " cupos libres"));

        if (alertas.isEmpty()) {
            pilaAlertas.add(FilaAlerta.textoAjustable(
                    "Nada pendiente. El albergue cumple los mínimos de ocupación, agua, espacio y stock."));
        }
        for (Alerta alerta : alertas) {
            FilaAlerta fila = filaConAccion(alerta);
            fila.setAlignmentX(Component.LEFT_ALIGNMENT);
            pilaAlertas.add(fila);
        }
        pilaAlertas.revalidate();
        pilaAlertas.repaint();

        a.getDamnificados().forEach(modeloDamnificados::addElement);
        modeloLotes.setDatos(new ArrayList<>(a.getRecursos()));
        actualizarMosaicos(a);
    }

    /** Cada alerta trae la acción que la resuelve, con el albergue ya elegido. */
    private FilaAlerta filaConAccion(Alerta alerta) {
        return switch (alerta.codigo()) {
            case "OCUPACION", "ESPACIO" -> new FilaAlerta(alerta, "Derivar personas", contexto::derivar);
            case "AGUA", "RECURSO_FALTANTE" -> new FilaAlerta(alerta,
                    "Registrar " + Tema.paleta(alerta.recurso()).nombre().toLowerCase(),
                    () -> contexto.registrarLote(alerta.recurso()));
            case "RECURSO_VENCIDO" -> new FilaAlerta(alerta, "Revisar inventario",
                    () -> contexto.ir(Contexto.Seccion.RECURSOS));
            default -> new FilaAlerta(alerta, null, null); // CAPACIDAD: es un dato del albergue, no una tarea
        };
    }

    private void actualizarMosaicos(Albergue a) {
        LocalDate hoy = LocalDate.now();
        int ocupantes = a.getOcupacionActual();
        for (TipoRecurso tipo : TipoRecurso.values()) {
            double total = a.getCantidadTotal(tipo, hoy);
            Set<String> unidades = new LinkedHashSet<>();
            int lotes = 0;
            int vencidos = 0;
            for (Recurso r : a.getRecursos()) {
                if (r.getTipo() == tipo) {
                    lotes++;
                    unidades.add(r.getUnidad());
                    if (r.estaVencido(hoy)) {
                        vencidos++;
                    }
                }
            }
            String unidad = unidades.size() == 1 ? " " + unidades.iterator().next() : "";
            String cifra = Formatos.numero(total) + unidad;
            String detalle;
            boolean alerta = tipo.esCritico() && ocupantes > 0 && total <= 0;
            if (tipo == TipoRecurso.AGUA && ocupantes > 0) {
                double porPersona = total / ocupantes;
                detalle = Formatos.numero(porPersona) + " L por persona";
                alerta = porPersona < com.shelter.config.SphereConstantes.AGUA_LITROS_PERSONA_DIA_MIN;
            } else if (alerta) {
                detalle = "Falta (crítico)";
            } else if (lotes == 0) {
                detalle = "Sin lotes";
            } else {
                detalle = lotes + (lotes == 1 ? " lote" : " lotes") + (vencidos > 0 ? " · " + vencidos + " vencido(s)" : "");
            }
            mosaicos[tipo.ordinal()].setDatos(cifra, detalle, alerta);
        }
    }

    // =========================================================
    // ACCIONES
    // =========================================================

    private void dialogoNuevoAlbergue() {
        JTextField txtId = Formularios.campo("");
        JTextField txtNombre = Formularios.campo("");
        JTextField txtDistritoNuevo = Formularios.campo("");
        JTextField txtTipo = Formularios.campo("Coliseo");
        JSpinner spnCapacidad = new JSpinner(new SpinnerNumberModel(50, 1, 100000, 1));
        JTextField txtArea = Formularios.campo("200");
        Formularios.AvisoEnLinea aviso = new Formularios.AvisoEnLinea();
        Formularios.Rejilla form = new Formularios.Rejilla()
                .fila("ID (ej. A3)", txtId)
                .fila("Nombre", txtNombre)
                .fila("Distrito", txtDistritoNuevo)
                .fila("Infraestructura", txtTipo)
                .fila("Capacidad máxima", spnCapacidad)
                .fila("Área en m² (0 = desconocida)", txtArea)
                .completa(Formularios.nota("El nombre define el prefijo de los códigos (ej. Refugio Central → REFCE-00001)."))
                .completa(aviso);
        form.setPreferredSize(new Dimension(460, form.getPreferredSize().height + 40));

        while (JOptionPane.showConfirmDialog(this, form, "Registrar nuevo albergue",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                double area = Formularios.decimal(txtArea, "El área", true);
                Albergue nuevo = new Albergue(txtId.getText(), txtNombre.getText(), txtDistritoNuevo.getText(),
                        txtTipo.getText(), (Integer) spnCapacidad.getValue(), area);
                if (!controller.registrarAlbergue(nuevo)) {
                    aviso.error("Ya existe un albergue con el ID " + nuevo.getId() + ".", txtId);
                    continue; // el formulario vuelve con lo escrito y el campo marcado
                }
                contexto.setIdAlbergue(nuevo.getId());
                alMostrar();
                return;
            } catch (Formularios.EntradaInvalida ex) {
                aviso.error(ex);
            } catch (IllegalArgumentException ex) {
                aviso.error(ex.getMessage(), null);
            }
        }
    }

    /** RF-06 con el mismo lenguaje que las alertas del tablero (antes era un volcado de texto). */
    private void reporteSphere() {
        FilaAlerta.Pila pila = new FilaAlerta.Pila();
        for (Albergue a : controller.getAlbergues()) {
            JLabel titulo = new JLabel(a.getNombre() + "  ·  " + a.getId());
            titulo.setFont(Tema.SUBTITULO);
            titulo.setForeground(Tema.TEXTO);
            titulo.setBorder(BorderFactory.createEmptyBorder(14, 0, 4, 0));
            titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
            pila.add(titulo);
            List<Alerta> alertas = controller.minimosSphere(a);
            if (alertas.isEmpty()) {
                javax.swing.JTextArea ok = FilaAlerta.textoAjustable("Cumple los mínimos Sphere evaluados (agua y espacio vital).");
                ok.setForeground(Tema.mezclar(Tema.VERDE, Color.WHITE, 0.3));
                ok.setAlignmentX(Component.LEFT_ALIGNMENT);
                pila.add(ok);
            }
            for (Alerta al : alertas) {
                FilaAlerta f = new FilaAlerta(al, null, null);
                f.setAlignmentX(Component.LEFT_ALIGNMENT);
                pila.add(f);
            }
        }
        JScrollPane sp = Tema.scroll(pila);
        sp.setPreferredSize(new Dimension(620, 440));
        sp.getViewport().setBackground(Tema.SUPERFICIE);
        JOptionPane.showMessageDialog(this, sp, "Mínimos Sphere de la red", JOptionPane.PLAIN_MESSAGE);
    }

    // =========================================================
    // MODELOS DE TABLA
    // =========================================================

    private static final class ModeloAlbergues extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private static final String[] COLUMNAS = {"ID", "Nombre", "Distrito", "Tipo", "Área",
                "Ocupación", "Cupos libres", "Estado"};
        private transient List<Albergue> datos = List.of();

        void setDatos(List<Albergue> nuevos) {
            datos = List.copyOf(nuevos);
            fireTableDataChanged();
        }

        Albergue get(int fila) {
            return datos.get(fila);
        }

        @Override public int getRowCount() { return datos.size(); }
        @Override public int getColumnCount() { return COLUMNAS.length; }
        @Override public String getColumnName(int c) { return COLUMNAS[c]; }

        @Override
        public Object getValueAt(int fila, int col) {
            Albergue a = datos.get(fila);
            return switch (col) {
                case 0 -> a.getId();
                case 1 -> a.getNombre();
                case 2 -> a.getDistrito();
                case 3 -> a.getTipoInfraestructura();
                case 4 -> a.getAreaM2() > 0 ? Formatos.numero(a.getAreaM2()) + " m²" : "—";
                case 5 -> a.getPorcentajeOcupacion();
                // "0 de 2" leído bajo "Cupos libres": no se confunde con personas alojadas
                case 6 -> a.getCuposDisponibles() + " de " + a.getCapacidadMaxima();
                default -> a.getEstadoSemaforo();
            };
        }
    }

    private static final class ModeloLotes extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private static final String[] COLUMNAS = {"Lote", "Stock", "Vence"};
        private transient List<Recurso> datos = List.of();

        void setDatos(List<Recurso> nuevos) {
            datos = List.copyOf(nuevos);
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return datos.size(); }
        @Override public int getColumnCount() { return COLUMNAS.length; }
        @Override public String getColumnName(int c) { return COLUMNAS[c]; }

        @Override
        public Object getValueAt(int fila, int col) {
            Recurso r = datos.get(fila);
            return switch (col) {
                case 0 -> r;
                case 1 -> Formatos.cantidad(r.getCantidadDisponible(), r.getUnidad());
                default -> r.getFechaVencimiento() == null ? "No vence"
                        : (r.estaVencido(LocalDate.now()) ? "Vencido" : r.getFechaVencimiento().format(
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            };
        }
    }
}
