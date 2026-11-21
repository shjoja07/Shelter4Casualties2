package com.shelter.ui.vistas;

import com.shelter.controller.ShelterController;
import com.shelter.model.Albergue;
import com.shelter.model.Damnificado;
import com.shelter.model.Recurso;
import com.shelter.model.RegistroAsignacion;
import com.shelter.model.TipoRecurso;
import com.shelter.service.AlbergueService;
import com.shelter.ui.componentes.BotonModerno;
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
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Inventario y reparto: registro y actualización de lotes (RF-03), tabla de inventario con colores
 * por tipo de recurso y reparto priorizado con vista previa de la cola (RF-04).
 */
public class PanelRecursos extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient ShelterController controller;
    private boolean refrescando;

    // Registro de lotes
    private final JComboBox<Albergue> cmbAlbergueInventario = new JComboBox<>();
    private final JComboBox<TipoRecurso> cmbTipo = new JComboBox<>(TipoRecurso.values());
    private final JTextField txtNombre = Formularios.campo("Agua potable");
    private final JTextField txtUnidad = Formularios.campo(AlbergueService.UNIDAD_AGUA);
    private final JTextField txtCantidad = Formularios.campo("100");
    private final JTextField txtVence = Formularios.campo("");
    private final ModeloInventario modeloInventario = new ModeloInventario();

    // Reparto
    private final JComboBox<Albergue> cmbAlbergueReparto = new JComboBox<>();
    private final JComboBox<Recurso> cmbRecurso = new JComboBox<>();
    private final JTextField txtTotal = Formularios.campo("30");
    private final JTextField txtPorPersona = Formularios.campo("15");
    private final DefaultListModel<Damnificado> modeloCola = new DefaultListModel<>();
    private final JTextArea txtResultado = new JTextArea();

    public PanelRecursos(ShelterController controller) {
        super(new GridLayout(1, 2, 16, 0));
        this.controller = controller;
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
                actualizarTablaInventario();
            }
        });

        BotonModerno btnRegistrar = new BotonModerno("Registrar lote", BotonModerno.Estilo.PRIMARIO, new Color(0x047857));
        btnRegistrar.setIcon(Iconos.de(Iconos.Figura.CAJA, 16, Color.WHITE));
        btnRegistrar.addActionListener(e -> registrarLote());
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
                .fila("Vence (AAAA-MM-DD)", txtVence)
                .completa(botones);

        JTable tabla = new JTable(modeloInventario);
        Tema.estilizar(tabla);
        tabla.setRowHeight(30);
        tabla.setDefaultRenderer(Object.class, new Renderizadores.Texto());
        tabla.getColumnModel().getColumn(0).setCellRenderer(new Renderizadores.TipoRecursoCelda());
        tabla.getColumnModel().getColumn(4).setCellRenderer(new EstadoLoteCelda());

        JPanel columna = new JPanel(new BorderLayout(0, 14));
        columna.setOpaque(false);
        columna.add(new Tarjeta("Registrar lote en inventario (RF-03)", Tema.paleta(TipoRecurso.ALIMENTO).principal())
                .contenido(form), BorderLayout.NORTH);
        columna.add(new Tarjeta("Inventario del albergue", Tema.paleta(TipoRecurso.AGUA).secundario())
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
    }

    private void registrarLote() {
        try {
            Albergue albergue = Formularios.albergue(cmbAlbergueInventario);
            TipoRecurso tipo = (TipoRecurso) cmbTipo.getSelectedItem();
            String nombre = Formularios.textoObligatorio(txtNombre, "El nombre del recurso");
            String unidad = Formularios.textoObligatorio(txtUnidad, "La unidad");
            double cantidad = Formularios.decimal(txtCantidad, "La cantidad", false);
            LocalDate vence = Formularios.fechaOpcional(txtVence);
            if (controller.registrarRecurso(albergue.getId(), new Recurso(tipo, nombre, unidad, cantidad, vence))) {
                txtCantidad.setText("");
                txtVence.setText("");
                Formularios.info(this, "Lote registrado", String.format(Locale.US,
                        "Se registraron %.2f %s de %s en %s.", cantidad, unidad, nombre, albergue.getNombre()));
            } else {
                Formularios.error(this, "El albergue seleccionado ya no existe.");
            }
        } catch (Formularios.EntradaInvalida | IllegalArgumentException ex) {
            Formularios.error(this, ex.getMessage());
        }
    }

    private void modificarLote() {
        Albergue albergue = (Albergue) cmbAlbergueInventario.getSelectedItem();
        if (albergue == null || albergue.getRecursos().isEmpty()) {
            Formularios.aviso(this, "Este albergue no tiene lotes registrados.");
            return;
        }
        JComboBox<Recurso> cmbLote = new JComboBox<>(albergue.getRecursos().toArray(new Recurso[0]));
        cmbLote.setRenderer(new Renderizadores.RecursoCombo());
        JTextField txtNuevaCantidad = Formularios.campo("");
        JTextField txtNuevaFecha = Formularios.campo("");
        Runnable cargar = () -> {
            Recurso r = (Recurso) cmbLote.getSelectedItem();
            if (r != null) {
                txtNuevaCantidad.setText(String.valueOf(r.getCantidadDisponible()));
                txtNuevaFecha.setText(r.getFechaVencimiento() == null ? "" : r.getFechaVencimiento().toString());
            }
        };
        cmbLote.addActionListener(e -> cargar.run());
        cargar.run();
        Formularios.Rejilla form = new Formularios.Rejilla()
                .fila("Lote", cmbLote)
                .fila("Nueva cantidad", txtNuevaCantidad)
                .fila("Vence (vacío = no vence)", txtNuevaFecha);
        while (JOptionPane.showConfirmDialog(this, form, "Actualizar inventario de " + albergue.getNombre(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                Recurso r = (Recurso) cmbLote.getSelectedItem();
                double cantidad = Formularios.decimal(txtNuevaCantidad, "La cantidad", true);
                LocalDate vence = Formularios.fechaOpcional(txtNuevaFecha);
                if (controller.actualizarRecurso(albergue.getId(), r, cantidad, vence)) {
                    Formularios.info(this, "Inventario actualizado", r.toString());
                } else {
                    Formularios.error(this, "El lote ya no pertenece a este albergue.");
                }
                return;
            } catch (Formularios.EntradaInvalida | IllegalArgumentException ex) {
                Formularios.error(this, ex.getMessage());
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
                recargarRecursosReparto();
            }
        });
        cmbRecurso.addActionListener(e -> {
            if (!refrescando) {
                // Solo al elegir otro recurso se sugiere la cantidad: un refresco no pisa lo escrito
                Recurso r = (Recurso) cmbRecurso.getSelectedItem();
                if (r != null) {
                    txtPorPersona.setText(String.valueOf(controller.cantidadSugerida(r)));
                }
                actualizarCola();
            }
        });

        Formularios.Rejilla form = new Formularios.Rejilla()
                .fila("Albergue", cmbAlbergueReparto)
                .fila("Recurso", cmbRecurso)
                .fila("Cantidad total a repartir", txtTotal)
                .fila("Cantidad por persona", txtPorPersona)
                .completa(Formularios.nota("Agua: 15 L por persona (mínimo Sphere). La medicina solo se entrega a quien "
                        + "la necesita (atención médica, gestantes y enfermos crónicos)."));

        JList<Damnificado> cola = Renderizadores.lista(modeloCola, new Renderizadores.DamnificadoLista(true));

        txtResultado.setEditable(false);
        txtResultado.setFont(Tema.mono(Font.PLAIN, 12));
        txtResultado.setBackground(Tema.CAMPO);
        txtResultado.setForeground(Tema.TEXTO);
        txtResultado.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        txtResultado.setText("Aún no se ha realizado ningún reparto en esta sesión.");

        BotonModerno btnRepartir = new BotonModerno("Ejecutar reparto priorizado", BotonModerno.Estilo.PRIMARIO, new Color(0xB45309));
        btnRepartir.setIcon(Iconos.de(Iconos.Figura.CAMION, 16, Color.WHITE));
        btnRepartir.addActionListener(e -> repartir());

        JPanel listas = new JPanel(new GridLayout(2, 1, 0, 10));
        listas.setOpaque(false);
        listas.add(conTitulo("ORDEN DE PRIORIDAD (QUIÉN RECIBE PRIMERO)", Tema.scroll(cola)));
        listas.add(conTitulo("RESULTADO DEL ÚLTIMO REPARTO", Tema.scroll(txtResultado)));

        JPanel cuerpo = new JPanel(new BorderLayout(0, 12));
        cuerpo.setOpaque(false);
        cuerpo.add(form, BorderLayout.NORTH);
        cuerpo.add(listas, BorderLayout.CENTER);
        cuerpo.add(btnRepartir, BorderLayout.SOUTH);
        return new Tarjeta("Reparto priorizado de recursos escasos (RF-04)", new Color(0xF59E0B)).contenido(cuerpo);
    }

    private static JPanel conTitulo(String titulo, JComponent contenido) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.add(Formularios.etiqueta(titulo), BorderLayout.NORTH);
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
            txtPorPersona.setText(String.valueOf(controller.cantidadSugerida(r)));
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

    private void repartir() {
        try {
            Albergue albergue = Formularios.albergue(cmbAlbergueReparto);
            Recurso recurso = (Recurso) cmbRecurso.getSelectedItem();
            if (recurso == null) {
                throw new Formularios.EntradaInvalida("El albergue no tiene recursos para repartir.");
            }
            double total = Formularios.decimal(txtTotal, "La cantidad total", false);
            double porPersona = Formularios.decimal(txtPorPersona, "La cantidad por persona", false);
            List<RegistroAsignacion> entregas = controller.repartir(albergue, recurso, total, porPersona);
            StringBuilder sb = new StringBuilder(String.format("%d entrega(s) de %s:%n", entregas.size(), recurso.getNombre()));
            for (RegistroAsignacion r : entregas) {
                sb.append(String.format(Locale.US, "  → %-12s %-24s %8.2f %s%n", r.getIdDamnificado(),
                        recortar(r.getNombreDamnificado(), 24), r.getCantidad(), r.getUnidad()));
            }
            if (entregas.isEmpty()) {
                sb.append(recurso.getTipo() == TipoRecurso.MEDICINA
                        ? "Nadie en este albergue necesita medicina."
                        : "No había personas alojadas o el stock está en cero.");
            }
            sb.append(String.format(Locale.US, "%nStock restante: %.2f %s", recurso.getCantidadDisponible(), recurso.getUnidad()));
            txtResultado.setText(sb.toString());
            txtResultado.setCaretPosition(0);
        } catch (Formularios.EntradaInvalida | IllegalArgumentException ex) {
            Formularios.error(this, ex.getMessage());
        }
    }

    private static String recortar(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    // =========================================================
    // REFRESCO
    // =========================================================

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
        repaint();
    }

    // =========================================================
    // MODELO Y RENDERER DE LA TABLA DE INVENTARIO
    // =========================================================

    private static final class ModeloInventario extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private static final String[] COLUMNAS = {"Tipo", "Nombre", "Stock", "Vencimiento", "Estado"};
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
                case 0 -> r.getTipo();
                case 1 -> r.getNombre();
                case 2 -> String.format("%.2f %s", r.getCantidadDisponible(), r.getUnidad());
                case 3 -> r.getFechaVencimiento() == null ? "No vence" : r.getFechaVencimiento().toString();
                default -> r.estaVencido(LocalDate.now()) ? "VENCIDO" : (r.getCantidadDisponible() <= 0 ? "AGOTADO" : "VIGENTE");
            };
        }
    }

    private static final class EstadoLoteCelda implements TableCellRenderer {
        private final Renderizadores.Chip chip = new Renderizadores.Chip();

        @Override
        public java.awt.Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            String estado = String.valueOf(v);
            Color color = switch (estado) {
                case "VENCIDO" -> Tema.ROJO;
                case "AGOTADO" -> Tema.AMARILLO;
                default -> Tema.VERDE;
            };
            chip.configurar(estado, color, true);
            JPanel p = new JPanel(new BorderLayout());
            p.setBackground(s ? Tema.SELECCION : (r % 2 == 0 ? Tema.SUPERFICIE : new Color(0x121C2E)));
            p.add(chip);
            return p;
        }
    }
}
