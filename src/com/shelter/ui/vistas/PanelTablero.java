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
import com.shelter.ui.componentes.Formularios;
import com.shelter.ui.componentes.Iconos;
import com.shelter.ui.componentes.Renderizadores;
import com.shelter.ui.componentes.SemaforoComponente;
import com.shelter.ui.componentes.Tarjeta;
import com.shelter.ui.componentes.TarjetaRecurso;
import com.shelter.ui.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Tablero principal del centro de comando: semáforo del albergue seleccionado, tabla de la red con
 * filtros (RF-05, RF-07), mosaicos de recursos por tipo y detalle de alertas, damnificados e
 * inventario (RF-06, RF-09).
 */
public class PanelTablero extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient ShelterController controller;

    // Semáforo y resumen del albergue seleccionado
    private final SemaforoComponente semaforo = new SemaforoComponente();
    private final JLabel lblNombre = new JLabel("Sin albergue");
    private final JLabel lblUbicacion = new JLabel(" ");
    private final Renderizadores.Chip chipEstado = new Renderizadores.Chip();
    private final BarraNivel barraOcupacion = new BarraNivel();
    private final JLabel lblOcupacion = new JLabel(" ");
    private final JTextArea txtMotivo = new JTextArea();
    private final Tarjeta tarjetaSemaforo = new Tarjeta("Semáforo del albergue");

    // Tabla de la red + filtros
    private final ModeloAlbergues modeloAlbergues = new ModeloAlbergues();
    private final JTable tablaAlbergues = new JTable(modeloAlbergues);
    private final JTextField txtDistrito = Formularios.campo("");
    private final JSpinner spnCupos = new JSpinner(new SpinnerNumberModel(0, 0, 100000, 1));
    private final JComboBox<Object> cmbNivel = new JComboBox<>();
    private final JComboBox<Object> cmbSinStock = new JComboBox<>();

    // Detalle
    private final TarjetaRecurso[] mosaicos = new TarjetaRecurso[TipoRecurso.values().length];
    private final DefaultListModel<Alerta> modeloAlertas = new DefaultListModel<>();
    private final DefaultListModel<Damnificado> modeloDamnificados = new DefaultListModel<>();
    private final ModeloLotes modeloLotes = new ModeloLotes();
    private final Tarjeta tarjetaAlertas = new Tarjeta("Alertas activas", Tema.ROJO);
    private final Tarjeta tarjetaDamnificados = new Tarjeta("Damnificados alojados", new Color(0x2DD4BF));

    private boolean refrescando;

    public PanelTablero(ShelterController controller) {
        super(new BorderLayout(16, 0));
        this.controller = controller;
        setOpaque(false);
        add(construirColumnaSemaforo(), BorderLayout.WEST);
        add(construirCentro(), BorderLayout.CENTER);
    }

    // =========================================================
    // CONSTRUCCIÓN
    // =========================================================

    private JPanel construirColumnaSemaforo() {
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        lblNombre.setFont(Tema.fuente(Font.BOLD, 17));
        lblNombre.setForeground(Tema.TEXTO);
        lblUbicacion.setFont(Tema.TEXTO_PEQUENO);
        lblUbicacion.setForeground(Tema.TEXTO_SUAVE);
        chipEstado.setAlineacion(SwingConstants.LEFT);
        chipEstado.setFont(Tema.fuente(Font.BOLD, 12));
        lblOcupacion.setFont(Tema.TEXTO_PEQUENO);
        lblOcupacion.setForeground(Tema.TEXTO_SUAVE);
        barraOcupacion.setMostrarTexto(true);
        txtMotivo.setEditable(false);
        txtMotivo.setLineWrap(true);
        txtMotivo.setWrapStyleWord(true);
        txtMotivo.setOpaque(false);
        txtMotivo.setFont(Tema.TEXTO_PEQUENO);
        txtMotivo.setForeground(Tema.TEXTO_SUAVE);
        txtMotivo.setBorder(null);
        txtMotivo.setFocusable(false);

        for (Component c : new Component[]{lblNombre, lblUbicacion, chipEstado, barraOcupacion, lblOcupacion, txtMotivo}) {
            ((javax.swing.JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        chipEstado.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        barraOcupacion.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        info.add(lblNombre);
        info.add(lblUbicacion);
        info.add(javax.swing.Box.createVerticalStrut(10));
        info.add(chipEstado);
        info.add(javax.swing.Box.createVerticalStrut(12));
        info.add(etiquetaSeccion("OCUPACIÓN"));
        info.add(javax.swing.Box.createVerticalStrut(4));
        info.add(barraOcupacion);
        info.add(javax.swing.Box.createVerticalStrut(3));
        info.add(lblOcupacion);
        info.add(javax.swing.Box.createVerticalStrut(12));
        info.add(etiquetaSeccion("MOTIVO PRINCIPAL"));
        info.add(javax.swing.Box.createVerticalStrut(4));
        info.add(txtMotivo);

        JPanel contenido = new JPanel(new BorderLayout(0, 14));
        contenido.setOpaque(false);
        contenido.add(semaforo, BorderLayout.CENTER);
        contenido.add(info, BorderLayout.SOUTH);
        tarjetaSemaforo.contenido(contenido);
        tarjetaSemaforo.setPreferredSize(new Dimension(270, 100));
        return tarjetaSemaforo;
    }

    private static JLabel etiquetaSeccion(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(Tema.fuente(Font.BOLD, 10));
        l.setForeground(Tema.TEXTO_TENUE);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JPanel construirCentro() {
        JPanel centro = new JPanel(new GridBagLayout());
        centro.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;

        c.gridy = 0;
        c.weighty = 0.52;
        c.insets = new Insets(0, 0, 12, 0);
        centro.add(construirTablaRed(), c);

        c.gridy = 1;
        c.weighty = 0;
        JPanel filaMosaicos = new JPanel(new GridLayout(1, mosaicos.length, 10, 0));
        filaMosaicos.setOpaque(false);
        for (TipoRecurso tipo : TipoRecurso.values()) {
            mosaicos[tipo.ordinal()] = new TarjetaRecurso(tipo);
            filaMosaicos.add(mosaicos[tipo.ordinal()]);
        }
        centro.add(filaMosaicos, c);

        c.gridy = 2;
        c.weighty = 0.48;
        c.insets = new Insets(0, 0, 0, 0);
        centro.add(construirDetalle(), c);
        return centro;
    }

    private Tarjeta construirTablaRed() {
        Tarjeta tarjeta = new Tarjeta("Red de albergues · Panel semáforo");

        BotonModerno btnNuevo = new BotonModerno("Nuevo albergue");
        btnNuevo.setIcon(Iconos.de(Iconos.Figura.MAS, 14, Color.WHITE));
        btnNuevo.addActionListener(e -> dialogoNuevoAlbergue());
        BotonModerno btnSphere = new BotonModerno("Validar Sphere", BotonModerno.Estilo.SECUNDARIO);
        btnSphere.setIcon(Iconos.de(Iconos.Figura.ESCUDO, 14, Tema.ACENTO));
        btnSphere.addActionListener(e -> reporteSphere());
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.setOpaque(false);
        acciones.add(btnSphere);
        acciones.add(btnNuevo);
        tarjeta.accionesTitulo(acciones);

        // Filtros (RF-07): se aplican en vivo, sin botón "Filtrar"
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
        txtDistrito.setColumns(10);
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
        filtros.add(Formularios.etiqueta("Semáforo"));
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
        int[] anchos = {55, 160, 110, 115, 75, 150, 85, 115};
        for (int i = 0; i < anchos.length; i++) {
            tablaAlbergues.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        tablaAlbergues.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !refrescando) {
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
        JPanel fila = new JPanel(new GridBagLayout());
        fila.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;

        JList<Alerta> listaAlertas = Renderizadores.lista(modeloAlertas, new Renderizadores.AlertaLista());
        c.weightx = 0.36;
        c.insets = new Insets(0, 0, 0, 12);
        fila.add(tarjetaAlertas.contenido(Tema.scroll(listaAlertas)), c);

        JList<Damnificado> listaDamnificados = Renderizadores.lista(modeloDamnificados, new Renderizadores.DamnificadoLista(false));
        c.weightx = 0.34;
        fila.add(tarjetaDamnificados.contenido(Tema.scroll(listaDamnificados)), c);

        JTable tablaLotes = new JTable(modeloLotes);
        Tema.estilizar(tablaLotes);
        tablaLotes.setRowHeight(30);
        tablaLotes.setDefaultRenderer(Object.class, new Renderizadores.Texto());
        tablaLotes.getColumnModel().getColumn(0).setCellRenderer(new Renderizadores.LoteCelda());
        tablaLotes.getColumnModel().getColumn(0).setPreferredWidth(150);
        c.weightx = 0.30;
        c.insets = new Insets(0, 0, 0, 0);
        fila.add(new Tarjeta("Inventario (lotes)", Tema.paleta(TipoRecurso.ALIMENTO).principal()).contenido(Tema.scroll(tablaLotes)), c);
        // Las tres tarjetas reparten el ancho por peso, no por su contenido
        for (Component hijo : fila.getComponents()) {
            hijo.setPreferredSize(new Dimension(10, 10));
        }
        return fila;
    }

    // =========================================================
    // REFRESCO (se llama cada vez que el controlador avisa un cambio)
    // =========================================================

    public void refrescar() {
        String seleccionado = albergueSeleccionado() == null ? null : albergueSeleccionado().getId();
        refrescando = true;
        try {
            modeloAlbergues.setDatos(controller.filtrar(filtroActual()));
            int fila = -1;
            for (int i = 0; i < modeloAlbergues.getRowCount(); i++) {
                if (modeloAlbergues.get(i).getId().equalsIgnoreCase(String.valueOf(seleccionado))) {
                    fila = i;
                }
            }
            if (fila < 0 && modeloAlbergues.getRowCount() > 0) {
                fila = 0;
            }
            if (fila >= 0) {
                tablaAlbergues.setRowSelectionInterval(fila, fila);
            }
        } finally {
            refrescando = false;
        }
        mostrarDetalle();
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
        modeloAlertas.clear();
        modeloDamnificados.clear();
        if (a == null) {
            lblNombre.setText("Sin albergue");
            lblUbicacion.setText("Ningún albergue coincide con los filtros");
            chipEstado.configurar("SIN DATOS", Tema.BORDE, false);
            barraOcupacion.setPorcentaje(0, "");
            lblOcupacion.setText(" ");
            txtMotivo.setText("");
            modeloLotes.setDatos(List.of());
            for (TarjetaRecurso m : mosaicos) {
                m.setDatos("—", "", false);
            }
            return;
        }

        NivelSemaforo nivel = a.getEstadoSemaforo();
        semaforo.setNivel(nivel);
        tarjetaSemaforo.setAcento(Tema.color(nivel));
        lblNombre.setText(a.getNombre());
        lblUbicacion.setText(a.getId() + " · " + a.getDistrito());
        lblUbicacion.setToolTipText(a.getTipoInfraestructura() + " · prefijo de códigos " + a.getPrefijo());
        chipEstado.configurar("ESTADO " + nivel.getEtiqueta(), Tema.color(nivel), true);
        barraOcupacion.setPorcentaje(a.getPorcentajeOcupacion(), Math.round(a.getPorcentajeOcupacion()) + "%");
        lblOcupacion.setText(a.getOcupacionActual() + " de " + a.getCapacidadMaxima() + " personas · "
                + a.getCuposDisponibles() + " libres");

        List<Alerta> alertas = controller.alertas(a);
        alertas.forEach(modeloAlertas::addElement);
        tarjetaAlertas.setAcento(Tema.color(nivel));
        txtMotivo.setText(alertas.stream()
                .filter(al -> al.gravedad() == nivel)
                .map(Alerta::mensaje)
                .findFirst()
                .orElse("Sin alertas: el albergue cumple los mínimos evaluados (ocupación, agua, espacio y stock)."));

        a.getDamnificados().forEach(modeloDamnificados::addElement);
        modeloLotes.setDatos(new ArrayList<>(a.getRecursos()));
        actualizarMosaicos(a);
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
            String unidad = unidades.size() == 1 ? " " + unidades.iterator().next() : (unidades.isEmpty() ? "" : " (varias unid.)");
            String cifra = String.format(Locale.US, total % 1 == 0 ? "%.0f%s" : "%.1f%s", total, unidad);
            String detalle;
            boolean alerta = tipo.esCritico() && ocupantes > 0 && total <= 0;
            if (tipo == TipoRecurso.AGUA && ocupantes > 0) {
                double porPersona = total / ocupantes;
                detalle = String.format(Locale.US, "%.1f L/persona", porPersona);
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
        Formularios.Rejilla form = new Formularios.Rejilla()
                .fila("ID (ej. A3)", txtId)
                .fila("Nombre", txtNombre)
                .fila("Distrito", txtDistritoNuevo)
                .fila("Infraestructura", txtTipo)
                .fila("Capacidad máxima", spnCapacidad)
                .fila("Área en m² (0 = desconocida)", txtArea)
                .completa(Formularios.nota("El nombre define el prefijo de los códigos (ej. Refugio Central → REFCE-00001)."));
        form.setPreferredSize(new Dimension(420, form.getPreferredSize().height));

        while (javax.swing.JOptionPane.showConfirmDialog(this, form, "Registrar nuevo albergue (RF-01)",
                javax.swing.JOptionPane.OK_CANCEL_OPTION, javax.swing.JOptionPane.PLAIN_MESSAGE) == javax.swing.JOptionPane.OK_OPTION) {
            try {
                double area = Formularios.decimal(txtArea, "El área", true);
                Albergue nuevo = new Albergue(txtId.getText(), txtNombre.getText(), txtDistritoNuevo.getText(),
                        txtTipo.getText(), (Integer) spnCapacidad.getValue(), area);
                if (!controller.registrarAlbergue(nuevo)) {
                    Formularios.error(this, "Ya existe un albergue con el ID " + nuevo.getId() + ".");
                    continue;
                }
                seleccionar(nuevo.getId());
                List<Alerta> sphere = controller.minimosSphere(nuevo);
                StringBuilder msg = new StringBuilder("Albergue registrado: " + nuevo.getNombre()
                        + "\nPrefijo de códigos: " + nuevo.getPrefijo());
                sphere.forEach(al -> msg.append("\n• ").append(al));
                Formularios.info(this, "Albergue registrado", msg.toString());
                return;
            } catch (Formularios.EntradaInvalida | IllegalArgumentException ex) {
                Formularios.error(this, ex.getMessage()); // se vuelve a mostrar el formulario con lo escrito
            }
        }
    }

    private void seleccionar(String idAlbergue) {
        for (int i = 0; i < modeloAlbergues.getRowCount(); i++) {
            if (modeloAlbergues.get(i).getId().equalsIgnoreCase(idAlbergue)) {
                tablaAlbergues.setRowSelectionInterval(i, i);
                tablaAlbergues.scrollRectToVisible(tablaAlbergues.getCellRect(i, 0, true));
            }
        }
    }

    private void reporteSphere() {
        StringBuilder sb = new StringBuilder();
        for (Albergue a : controller.getAlbergues()) {
            sb.append("■ ").append(a.getNombre()).append(" [").append(a.getId()).append("]\n");
            List<Alerta> alertas = controller.minimosSphere(a);
            if (alertas.isEmpty()) {
                sb.append("   ✓ Cumple los mínimos Sphere evaluados (agua y espacio vital).\n");
            }
            alertas.forEach(al -> sb.append("   ! ").append(al).append('\n'));
            sb.append('\n');
        }
        JTextArea area = new JTextArea(sb.toString(), 18, 64);
        area.setEditable(false);
        area.setFont(Tema.mono(Font.PLAIN, 12));
        area.setBackground(Tema.CAMPO);
        area.setForeground(Tema.TEXTO);
        area.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        javax.swing.JOptionPane.showMessageDialog(this, new javax.swing.JScrollPane(area),
                "Mínimos Sphere de la red (RF-06)", javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }

    // =========================================================
    // MODELOS DE TABLA
    // =========================================================

    private static final class ModeloAlbergues extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private static final String[] COLUMNAS = {"ID", "Nombre", "Distrito", "Infraestructura", "Área m²",
                "Ocupación", "Cupos", "Semáforo"};
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
                case 4 -> a.getAreaM2() > 0 ? String.format("%.1f", a.getAreaM2()) : "—";
                case 5 -> a.getPorcentajeOcupacion();
                case 6 -> a.getCuposDisponibles() + " / " + a.getCapacidadMaxima();
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
                case 1 -> String.format(r.getCantidadDisponible() % 1 == 0 ? "%.0f %s" : "%.2f %s",
                        r.getCantidadDisponible(), r.getUnidad());
                default -> r.getFechaVencimiento() == null ? "—"
                        : (r.estaVencido(LocalDate.now()) ? "VENCIDO" : r.getFechaVencimiento().toString());
            };
        }
    }
}
