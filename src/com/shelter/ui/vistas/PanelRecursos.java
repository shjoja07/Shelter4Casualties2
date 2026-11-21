package com.shelter.ui.vistas;

import com.shelter.controller.ShelterController;
import com.shelter.model.Albergue;
import com.shelter.model.Damnificado;
import com.shelter.model.Recurso;
import com.shelter.model.RegistroAsignacion;
import com.shelter.model.TipoRecurso;
import com.shelter.service.AlbergueService;
import com.shelter.service.AsignacionRecurso.EntregaPlanificada;
import com.shelter.ui.componentes.BotonModerno;
import com.shelter.ui.componentes.Formatos;
import com.shelter.ui.componentes.Formularios;
import com.shelter.ui.componentes.Iconos;
import com.shelter.ui.componentes.Renderizadores;
import com.shelter.ui.componentes.Tarjeta;
import com.shelter.ui.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Inventario y reparto: registro y actualización de lotes (RF-03), inventario con colores por
 * tipo de recurso y reparto priorizado con vista previa de la cola (RF-04).
 *
 * Harden: el reparto ya no se ejecuta con un solo clic. Primero se muestra exactamente qué se
 * entregará y cuánto quedará, y después de ejecutarlo se puede deshacer. Las fechas se eligen
 * con un selector en lugar de escribirlas.
 */
public class PanelRecursos extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient ShelterController controller;
    private final transient Contexto contexto;
    private boolean refrescando;

    // Registro de lotes
    private final JComboBox<Albergue> cmbAlbergueInventario = new JComboBox<>();
    private final JComboBox<TipoRecurso> cmbTipo = new JComboBox<>(TipoRecurso.values());
    private final JTextField txtNombre = Formularios.campo("Agua potable");
    private final JTextField txtUnidad = Formularios.campo(AlbergueService.UNIDAD_AGUA);
    private final JTextField txtCantidad = Formularios.campo("100");
    private final Formularios.CampoFecha campoVence = new Formularios.CampoFecha();
    private final Formularios.AvisoEnLinea avisoLote = new Formularios.AvisoEnLinea();
    private final ModeloInventario modeloInventario = new ModeloInventario();

    // Reparto
    private final JComboBox<Albergue> cmbAlbergueReparto = new JComboBox<>();
    private final JComboBox<Recurso> cmbRecurso = new JComboBox<>();
    private final JTextField txtTotal = Formularios.campo("30");
    private final JTextField txtPorPersona = Formularios.campo("15");
    private final DefaultListModel<Damnificado> modeloCola = new DefaultListModel<>();
    private final JTextArea txtResultado = new JTextArea();
    private final Formularios.AvisoEnLinea avisoReparto = new Formularios.AvisoEnLinea();
    private final BotonModerno btnDeshacer = new BotonModerno("Deshacer último reparto", BotonModerno.Estilo.SECUNDARIO);

    public PanelRecursos(ShelterController controller, Contexto contexto) {
        super(new GridLayout(1, 2, 16, 0));
        this.controller = controller;
        this.contexto = contexto;
        setOpaque(false);
        cmbAlbergueInventario.setRenderer(new Renderizadores.AlbergueCombo());
        cmbAlbergueReparto.setRenderer(new Renderizadores.AlbergueCombo());
        cmbTipo.setRenderer(new Renderizadores.TipoRecursoCombo());
        cmbRecurso.setRenderer(new Renderizadores.RecursoCombo());
        add(construirInventario());
        add(construirReparto());
        actualizarUnidad();
    }

    // =========================================================
    // INVENTARIO (RF-03)
    // =========================================================

    private JPanel construirInventario() {
        cmbTipo.addActionListener(e -> actualizarUnidad());
        cmbAlbergueInventario.addActionListener(e -> {
            if (!refrescando) {
                recordarAlbergue(cmbAlbergueInventario);
                actualizarTablaInventario();
            }
        });

        BotonModerno btnRegistrar = new BotonModerno("Registrar lote", BotonModerno.Estilo.PRIMARIO);
        btnRegistrar.setIcon(Iconos.de(Iconos.Figura.CAJA, 16, Tema.TINTA_ACCION));
        btnRegistrar.setToolTipText("Enter en Nombre o Cantidad también registra");
        btnRegistrar.addActionListener(e -> registrarLote());
        for (JTextField t : new JTextField[]{txtNombre, txtUnidad, txtCantidad}) {
            t.addActionListener(e -> registrarLote());
        }
        BotonModerno btnModificar = new BotonModerno("Modificar lote…", BotonModerno.Estilo.SECUNDARIO);
        btnModificar.addActionListener(e -> modificarLote());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        botones.setOpaque(false);
        botones.add(btnModificar);
        botones.add(btnRegistrar);

        Formularios.Rejilla form = new Formularios.Rejilla()
                .fila("Albergue", cmbAlbergueInventario)
                .fila("Tipo de recurso", cmbTipo)
                .fila("Nombre", txtNombre)
                .fila("Unidad", txtUnidad)
                .fila("Cantidad", txtCantidad)
                .fila("Vencimiento", campoVence)
                .completa(botones)
                .completa(avisoLote);

        JTable tabla = Renderizadores.tabla(modeloInventario,
                () -> cmbAlbergueInventario.getSelectedItem() == null
                        ? "Registra un albergue en el Tablero para empezar."
                        : "Este albergue no tiene lotes. Registra el primero con el formulario de arriba.");
        Tema.estilizar(tabla);
        tabla.setDefaultRenderer(Object.class, new Renderizadores.Texto());
        tabla.getColumnModel().getColumn(0).setCellRenderer(new Renderizadores.LoteCelda());
        tabla.getColumnModel().getColumn(0).setPreferredWidth(190);
        tabla.getColumnModel().getColumn(3).setCellRenderer(new EstadoLoteCelda());

        Tarjeta tLote = new Tarjeta("Registrar lote");
        tLote.setToolTipText("RF-03");
        JPanel columna = new JPanel(new BorderLayout(0, 14));
        columna.setOpaque(false);
        columna.add(tLote.contenido(form), BorderLayout.NORTH);
        columna.add(new Tarjeta("Inventario del albergue")
                .contenido(Tema.scroll(tabla)), BorderLayout.CENTER);
        return columna;
    }

    /** El agua siempre se registra en litros (necesario para validar el mínimo Sphere). */
    private void actualizarUnidad() {
        TipoRecurso tipo = (TipoRecurso) cmbTipo.getSelectedItem();
        boolean esAgua = tipo == TipoRecurso.AGUA;
        if (esAgua) {
            txtUnidad.setText(AlbergueService.UNIDAD_AGUA);
        } else if (txtUnidad.getText().equals(AlbergueService.UNIDAD_AGUA)) {
            txtUnidad.setText("unidades");
        }
        txtUnidad.setEnabled(!esAgua);
        txtUnidad.setToolTipText(esAgua ? "El agua se registra en litros para compararla con el mínimo Sphere" : null);
    }

    private void registrarLote() {
        try {
            Albergue albergue = Formularios.albergue(cmbAlbergueInventario);
            TipoRecurso tipo = (TipoRecurso) cmbTipo.getSelectedItem();
            String nombre = Formularios.textoObligatorio(txtNombre, "El nombre del recurso");
            String unidad = Formularios.textoObligatorio(txtUnidad, "La unidad");
            double cantidad = Formularios.decimal(txtCantidad, "La cantidad", false);
            LocalDate vence = campoVence.getFecha();
            if (controller.registrarRecurso(albergue.getId(), new Recurso(tipo, nombre, unidad, cantidad, vence))) {
                avisoLote.exito("Registrado: " + Formatos.cantidad(cantidad, unidad) + " de " + nombre
                        + " en " + albergue.getNombre() + ".");
                txtCantidad.setText("");
                campoVence.setFecha(null);
            } else {
                avisoLote.error("El albergue elegido ya no existe.", cmbAlbergueInventario);
            }
        } catch (Formularios.EntradaInvalida ex) {
            avisoLote.error(ex);
        } catch (IllegalArgumentException ex) {
            avisoLote.error(ex.getMessage(), null);
        }
    }

    private void modificarLote() {
        Albergue albergue = (Albergue) cmbAlbergueInventario.getSelectedItem();
        if (albergue == null || albergue.getRecursos().isEmpty()) {
            avisoLote.error("Este albergue no tiene lotes para modificar. Registra uno primero.", cmbAlbergueInventario);
            return;
        }
        JComboBox<Recurso> cmbLote = new JComboBox<>(albergue.getRecursos().toArray(new Recurso[0]));
        cmbLote.setRenderer(new Renderizadores.RecursoCombo());
        JTextField txtNuevaCantidad = Formularios.campo("");
        Formularios.CampoFecha nuevaFecha = new Formularios.CampoFecha();
        Formularios.AvisoEnLinea aviso = new Formularios.AvisoEnLinea();
        Runnable cargar = () -> {
            Recurso r = (Recurso) cmbLote.getSelectedItem();
            if (r != null) {
                txtNuevaCantidad.setText(Formatos.numero(r.getCantidadDisponible()));
                nuevaFecha.setFecha(r.getFechaVencimiento());
            }
        };
        cmbLote.addActionListener(e -> cargar.run());
        cargar.run();
        Formularios.Rejilla form = new Formularios.Rejilla()
                .fila("Lote", cmbLote)
                .fila("Nueva cantidad", txtNuevaCantidad)
                .fila("Vencimiento", nuevaFecha)
                .completa(aviso);
        while (JOptionPane.showConfirmDialog(this, form, "Modificar lote en " + albergue.getNombre(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                Recurso r = (Recurso) cmbLote.getSelectedItem();
                double cantidad = Formularios.decimal(txtNuevaCantidad, "La cantidad", true);
                if (controller.actualizarRecurso(albergue.getId(), r, cantidad, nuevaFecha.getFecha())) {
                    avisoLote.exito("Lote actualizado: " + r.getNombre() + ", "
                            + Formatos.cantidad(r.getCantidadDisponible(), r.getUnidad()) + ".");
                } else {
                    avisoLote.error("Ese lote ya no pertenece a este albergue.", null);
                }
                return;
            } catch (Formularios.EntradaInvalida ex) {
                aviso.error(ex); // el diálogo vuelve con lo escrito y el campo marcado
            } catch (IllegalArgumentException ex) {
                aviso.error(ex.getMessage(), null);
            }
        }
    }

    private void actualizarTablaInventario() {
        Albergue a = (Albergue) cmbAlbergueInventario.getSelectedItem();
        modeloInventario.setDatos(a == null ? List.of() : new ArrayList<>(a.getRecursos()));
    }

    // =========================================================
    // REPARTO (RF-04)
    // =========================================================

    private JComponent construirReparto() {
        cmbAlbergueReparto.addActionListener(e -> {
            if (!refrescando) {
                recordarAlbergue(cmbAlbergueReparto);
                recargarRecursosReparto();
            }
        });
        cmbRecurso.addActionListener(e -> {
            if (!refrescando) {
                // Solo al elegir otro recurso se sugiere la cantidad: un refresco no pisa lo escrito
                Recurso r = (Recurso) cmbRecurso.getSelectedItem();
                if (r != null) {
                    txtPorPersona.setText(Formatos.numero(controller.cantidadSugerida(r)));
                }
                actualizarCola();
            }
        });
        txtTotal.addActionListener(e -> repartir());
        txtPorPersona.addActionListener(e -> repartir());

        Formularios.Rejilla form = new Formularios.Rejilla()
                .fila("Albergue", cmbAlbergueReparto)
                .fila("Recurso", cmbRecurso)
                .fila("Cantidad total a repartir", txtTotal)
                .fila("Cantidad por persona", txtPorPersona)
                .completa(Formularios.nota("Agua: 15 L por persona (mínimo Sphere). La medicina solo se entrega a quien "
                        + "la necesita: atención médica, gestantes y enfermedades crónicas."));

        JList<Damnificado> cola = Renderizadores.lista(modeloCola, new Renderizadores.DamnificadoLista(true),
                () -> "No hay personas alojadas en este albergue: no hay a quién repartir.");

        txtResultado.setEditable(false);
        txtResultado.setLineWrap(true);
        txtResultado.setWrapStyleWord(true);
        txtResultado.setFont(Tema.TEXTO_NORMAL);
        txtResultado.setBackground(Tema.CAMPO);
        txtResultado.setForeground(Tema.TEXTO);
        txtResultado.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        txtResultado.setText("Todavía no se ha hecho ningún reparto en esta sesión.");

        BotonModerno btnRepartir = new BotonModerno("Repartir…", BotonModerno.Estilo.PRIMARIO);
        btnRepartir.setIcon(Iconos.de(Iconos.Figura.CAMION, 16, Tema.TINTA_ACCION));
        btnRepartir.setToolTipText("Muestra el resumen y pide confirmación antes de descontar el stock");
        btnRepartir.addActionListener(e -> repartir());
        btnDeshacer.setEnabled(false);
        btnDeshacer.addActionListener(e -> deshacerReparto());
        JPanel botones = new JPanel(new BorderLayout(8, 0));
        botones.setOpaque(false);
        botones.add(btnDeshacer, BorderLayout.WEST);
        botones.add(btnRepartir, BorderLayout.CENTER);
        JPanel sur = new JPanel(new BorderLayout(0, 4));
        sur.setOpaque(false);
        sur.add(avisoReparto, BorderLayout.NORTH);
        sur.add(botones, BorderLayout.CENTER);

        JPanel listas = new JPanel(new GridLayout(2, 1, 0, 10));
        listas.setOpaque(false);
        listas.add(conTitulo("Quién recibe primero", Tema.scroll(cola)));
        listas.add(conTitulo("Último reparto", Tema.scroll(txtResultado)));

        JPanel cuerpo = new JPanel(new BorderLayout(0, 12));
        cuerpo.setOpaque(false);
        cuerpo.add(form, BorderLayout.NORTH);
        cuerpo.add(listas, BorderLayout.CENTER);
        cuerpo.add(sur, BorderLayout.SOUTH);
        Tarjeta tarjeta = new Tarjeta("Reparto por prioridad");
        tarjeta.setToolTipText("RF-04");
        return tarjeta.contenido(cuerpo);
    }

    private static JPanel conTitulo(String titulo, JComponent contenido) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        javax.swing.JLabel l = new javax.swing.JLabel(titulo);
        l.setFont(Tema.TEXTO_NEGRITA);
        l.setForeground(Tema.TEXTO_SUAVE);
        p.add(l, BorderLayout.NORTH);
        contenido.setBorder(BorderFactory.createLineBorder(Tema.BORDE_SUAVE));
        p.add(contenido, BorderLayout.CENTER);
        return p;
    }

    private void recargarRecursosReparto() {
        Albergue a = (Albergue) cmbAlbergueReparto.getSelectedItem();
        boolean antes = refrescando;
        refrescando = true;
        try {
            Formularios.recargar(cmbRecurso, a == null ? List.of() : new ArrayList<>(a.getRecursos()),
                    r -> r.getTipo() + "|" + r.getNombre() + "|" + r.getFechaVencimiento());
        } finally {
            refrescando = antes;
        }
        Recurso r = (Recurso) cmbRecurso.getSelectedItem();
        if (!antes && r != null) {
            txtPorPersona.setText(Formatos.numero(controller.cantidadSugerida(r)));
        }
        actualizarCola();
    }

    private void actualizarCola() {
        modeloCola.clear();
        Albergue a = (Albergue) cmbAlbergueReparto.getSelectedItem();
        if (a != null) {
            controller.ordenPrioridad(a, (Recurso) cmbRecurso.getSelectedItem()).forEach(modeloCola::addElement);
        }
    }

    /** Primero el resumen exacto, después la confirmación, y solo entonces se descuenta el stock. */
    private void repartir() {
        try {
            Albergue albergue = Formularios.albergue(cmbAlbergueReparto);
            Recurso recurso = (Recurso) cmbRecurso.getSelectedItem();
            if (recurso == null) {
                throw new Formularios.EntradaInvalida("Este albergue no tiene recursos para repartir. "
                        + "Registra un lote a la izquierda.", cmbRecurso);
            }
            double total = Formularios.decimal(txtTotal, "La cantidad total", false);
            double porPersona = Formularios.decimal(txtPorPersona, "La cantidad por persona", false);
            List<EntregaPlanificada> plan = controller.previsualizarReparto(albergue, recurso, total, porPersona);
            if (plan.isEmpty()) {
                avisoReparto.error(recurso.getTipo() == TipoRecurso.MEDICINA
                        ? "Nadie en este albergue necesita medicina: no hay a quién entregarla."
                        : "No hay personas alojadas o el stock está en cero.", cmbRecurso);
                return;
            }
            double aEntregar = plan.stream().mapToDouble(EntregaPlanificada::cantidad).sum();
            String unidad = recurso.getUnidad();
            StringBuilder resumen = new StringBuilder();
            resumen.append("Se entregarán ").append(Formatos.cantidad(aEntregar, unidad)).append(" de ")
                    .append(recurso.getNombre()).append(" a ").append(plan.size())
                    .append(plan.size() == 1 ? " persona" : " personas").append(":\n\n");
            for (EntregaPlanificada e : plan) {
                resumen.append("   ").append(e.damnificado().getId()).append("  ").append(e.damnificado().getNombreCompleto())
                        .append(" · ").append(Formatos.cantidad(e.cantidad(), unidad)).append('\n');
            }
            resumen.append("\nQuedarán ").append(Formatos.cantidad(recurso.getCantidadDisponible() - aEntregar, unidad))
                    .append(" en el inventario.");
            if (aEntregar + 1e-9 < total) {
                resumen.append("\nPediste repartir ").append(Formatos.cantidad(total, unidad))
                        .append(", pero solo alcanza para lo indicado arriba.");
            }
            if (!Formularios.confirmar(this, "Confirmar reparto", resumen.toString())) {
                return;
            }
            List<RegistroAsignacion> entregas = controller.repartir(albergue, recurso, total, porPersona);
            StringBuilder sb = new StringBuilder(entregas.size() + (entregas.size() == 1 ? " entrega" : " entregas")
                    + " de " + recurso.getNombre() + ":\n");
            for (RegistroAsignacion r : entregas) {
                sb.append("→ ").append(r.getIdDamnificado()).append("  ").append(r.getNombreDamnificado())
                        .append(" · ").append(Formatos.cantidad(r.getCantidad(), r.getUnidad())).append('\n');
            }
            sb.append("\nQuedan ").append(Formatos.cantidad(recurso.getCantidadDisponible(), recurso.getUnidad())).append('.');
            txtResultado.setText(sb.toString());
            txtResultado.setCaretPosition(0);
            avisoReparto.exito("Reparto registrado. Si fue un error, puedes deshacerlo.");
        } catch (Formularios.EntradaInvalida ex) {
            avisoReparto.error(ex);
        } catch (IllegalArgumentException ex) {
            avisoReparto.error(ex.getMessage(), null);
        }
    }

    private void deshacerReparto() {
        if (!controller.puedeDeshacerReparto()) {
            return;
        }
        if (!Formularios.confirmar(this, "Deshacer reparto",
                "¿Deshacer el último reparto? Se devolverá lo entregado al inventario y se borrarán sus constancias.")) {
            return;
        }
        try {
            double devuelto = controller.deshacerUltimoReparto();
            txtResultado.setText("Último reparto deshecho: volvieron " + Formatos.numero(devuelto) + " al inventario.");
            avisoReparto.exito("Reparto deshecho.");
        } catch (IllegalArgumentException ex) {
            avisoReparto.error(ex.getMessage(), null);
        }
    }

    // =========================================================
    // CONTEXTO COMPARTIDO Y REFRESCO
    // =========================================================

    private void recordarAlbergue(JComboBox<Albergue> combo) {
        if (combo.getSelectedItem() instanceof Albergue a) {
            contexto.setIdAlbergue(a.getId());
        }
    }

    /** Al mostrarse: los dos combos de albergue apuntan al albergue en foco. */
    public void alMostrar() {
        String id = contexto.getIdAlbergue();
        if (id == null) {
            return;
        }
        refrescando = true;
        try {
            for (JComboBox<Albergue> combo : List.of(cmbAlbergueInventario, cmbAlbergueReparto)) {
                for (int i = 0; i < combo.getItemCount(); i++) {
                    if (combo.getItemAt(i).getId().equalsIgnoreCase(id)) {
                        combo.setSelectedIndex(i);
                    }
                }
            }
            recargarRecursosReparto();
        } finally {
            refrescando = false;
        }
        actualizarTablaInventario();
        actualizarCola();
    }

    /** Llegada desde una alerta "Falta X": el formulario de lote ya viene con ese tipo elegido. */
    public void prepararLote(TipoRecurso tipo) {
        alMostrar();
        if (tipo != null) {
            cmbTipo.setSelectedItem(tipo);
            txtNombre.setText(Tema.paleta(tipo).nombre());
        }
        avisoLote.limpiar();
        txtCantidad.setText("");
        txtCantidad.requestFocusInWindow();
    }

    public void refrescar() {
        refrescando = true;
        try {
            List<Albergue> red = controller.getAlbergues();
            Formularios.recargar(cmbAlbergueInventario, red, Albergue::getId);
            Formularios.recargar(cmbAlbergueReparto, red, Albergue::getId);
            recargarRecursosReparto();
        } finally {
            refrescando = false;
        }
        actualizarTablaInventario();
        actualizarCola();
        btnDeshacer.setEnabled(controller.puedeDeshacerReparto());
        repaint();
    }

    // =========================================================
    // MODELO Y RENDERER DE LA TABLA DE INVENTARIO
    // =========================================================

    private static final class ModeloInventario extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private static final String[] COLUMNAS = {"Lote", "Stock", "Vencimiento", "Estado"};
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
                case 2 -> r.getFechaVencimiento() == null ? "No vence"
                        : r.getFechaVencimiento().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                default -> r.estaVencido(LocalDate.now()) ? "Vencido" : (r.getCantidadDisponible() <= 0 ? "Agotado" : "Vigente");
            };
        }
    }

    private static final class EstadoLoteCelda implements TableCellRenderer {
        private final Renderizadores.Chip chip = new Renderizadores.Chip();

        @Override
        public java.awt.Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            String estado = String.valueOf(v);
            Color color = switch (estado) {
                case "Vencido" -> Tema.ROJO;
                case "Agotado" -> Tema.AMARILLO;
                default -> Tema.VERDE;
            };
            if ("Vigente".equals(estado)) {
                chip.texto(estado, null); // vigente es lo normal: texto, sin pastilla
            } else {
                chip.configurar(estado, color, true);
            }
            JPanel p = new JPanel(new BorderLayout());
            p.setBackground(s ? Tema.SELECCION : (r % 2 == 0 ? Tema.SUPERFICIE : Tema.FILA_ALTERNA));
            p.add(chip);
            return p;
        }
    }
}
