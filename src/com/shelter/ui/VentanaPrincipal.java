package com.shelter.ui;

import com.shelter.model.Albergue;
import com.shelter.model.Alerta;
import com.shelter.model.Damnificado;
import com.shelter.model.DamnificadoGeneral;
import com.shelter.model.DamnificadoMenor;
import com.shelter.model.DamnificadoVulnerable;
import com.shelter.model.NivelSemaforo;
import com.shelter.model.Recurso;
import com.shelter.model.RegistroAsignacion;
import com.shelter.model.TipoRecurso;
import com.shelter.persistence.PersistenciaJson;
import com.shelter.service.AlbergueService;
import com.shelter.service.AlbergueService.ResultadoDerivacion;
import com.shelter.service.AlbergueService.ResultadoIngreso;
import com.shelter.service.AsignacionRecurso;
import com.shelter.service.FiltroAlbergue;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

public class VentanaPrincipal extends JFrame {

    private static final Path RUTA_DATOS = Path.of("datos_shelter.json");
    private static final DateTimeFormatter FMT_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Única instancia de los servicios para toda la aplicación
    private final AlbergueService albergueService = new AlbergueService();
    private final AsignacionRecurso asignacionService = new AsignacionRecurso(albergueService);

    // --- Componentes Pestaña 1: Panel Semáforo y Albergues ---
    private DefaultTableModel modeloTablaAlbergues;
    private JTable tablaAlbergues;
    private JTextField txtFiltroDistrito;
    private JSpinner spnFiltroCupos;
    private JComboBox<String> cmbFiltroNivel;
    private JComboBox<String> cmbFiltroRecurso;
    private DefaultListModel<String> modeloListaAlertas;
    private DefaultListModel<String> modeloListaDamnificadosDetalle;
    private DefaultListModel<String> modeloListaRecursosDetalle;
    private boolean filtroActivo = false;

    // --- Componentes Pestaña 2: Damnificados ---
    private JComboBox<String> cmbAlbergueIngreso;
    private JTextField txtDniIngreso;
    private JTextField txtNombreIngreso;
    private JSpinner spnEdadIngreso;
    private JCheckBox chkAtencionMedica;
    private JPanel panelCardsPerfil;
    // Subpanel Menor
    private JCheckBox chkTieneTutor;
    private JTextField txtDniTutor;
    private JTextField txtNombreTutor;
    private JTextField txtParentesco;
    // Subpanel Adulto
    private JComboBox<String> cmbTipoAdulto;
    private JTextField txtCondicionVulnerable;
    private JCheckBox chkGestante;
    private JCheckBox chkCronica;
    // Derivación y Salida
    private JComboBox<String> cmbAlbergueOrigen;
    private JComboBox<String> cmbAlbergueDestino;
    private JTextField txtDniDerivar;
    private JComboBox<String> cmbAlbergueSalida;
    private JTextField txtDniSalida;

    // --- Componentes Pestaña 3: Recursos y Reparto ---
    private JComboBox<String> cmbAlbergueRecurso;
    private JComboBox<TipoRecurso> cmbTipoRecurso;
    private JTextField txtNombreRecurso;
    private JTextField txtUnidadRecurso;
    private JTextField txtCantidadRecurso;
    private JTextField txtVencimientoRecurso;

    private JComboBox<String> cmbAlbergueAsignar;
    private JComboBox<Recurso> cmbRecursoAsignar;
    private JTextField txtCantTotalAsignar;
    private JTextField txtCantPorPersona;
    private DefaultListModel<String> modeloColaPrioridad;
    private JTextArea txtResultadoReparto;

    // --- Componentes Pestaña 4: Historial y JSON ---
    private DefaultTableModel modeloTablaHistorial;
    private JLabel lblEstadoArchivo;

    public VentanaPrincipal() {
        setTitle("Sistema Shelter 4 Casualties — Gestión de Albergues y Normas Sphere");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1180, 740);
        setLocationRelativeTo(null);

        iniciarDatos();

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.setFont(new Font("SansSerif", Font.BOLD, 13));
        pestanas.addTab("1. Panel Semáforo y Albergues", crearPanelAlbergues());
        pestanas.addTab("2. Gestión de Damnificados", crearPanelDamnificados());
        pestanas.addTab("3. Inventario y Reparto (Sphere)", crearPanelRecursos());
        pestanas.addTab("4. Historial y Persistencia (JSON)", crearPanelHistorialYJson());

        add(pestanas, BorderLayout.CENTER);

        // Guardado automático al cerrar la ventana
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                guardarSilencioso();
                dispose();
                System.exit(0);
            }
        });

        refrescarTodo();
    }

    // =========================================================================
    // INICIALIZACIÓN Y PERSISTENCIA BASE
    // =========================================================================
    private void iniciarDatos() {
        if (Files.exists(RUTA_DATOS)) {
            try {
                PersistenciaJson.Datos datos = PersistenciaJson.cargar(RUTA_DATOS);
                albergueService.reemplazarAlbergues(datos.albergues());
                asignacionService.reemplazarHistorial(datos.historial());
                return;
            } catch (IOException | IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this,
                        "No se pudo cargar datos_shelter.json (" + ex.getMessage() + ").\nSe iniciará con datos de prueba.",
                        "Aviso de Carga", JOptionPane.WARNING_MESSAGE);
            }
        }
        albergueService.registrarAlbergue(new Albergue("A1", "Refugio Central", "Lima Centro", "Coliseo", 2, 14));
        albergueService.registrarAlbergue(new Albergue("A2", "Refugio Norte", "Los Olivos", "Colegio", 5, 40));
    }

    private void guardarSilencioso() {
        try {
            PersistenciaJson.guardar(RUTA_DATOS, albergueService.getAlbergues(), asignacionService.getHistorial());
        } catch (IOException ignored) {
        }
    }

    // =========================================================================
    // PESTAÑA 1: PANEL SEMÁFORO Y ALBERGUES (RF-01, RF-05, RF-06, RF-07, RF-09)
    // =========================================================================
    private JPanel crearPanelAlbergues() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- Barra superior de filtros y acciones ---
        JPanel panelSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        panelSuperior.setBorder(new TitledBorder("Filtros de Red (RF-07) y Acciones de Albergue (RF-01 / RF-06)"));

        txtFiltroDistrito = new JTextField(10);
        spnFiltroCupos = new JSpinner(new SpinnerNumberModel(0, 0, 100000, 1));
        cmbFiltroNivel = new JComboBox<>(new String[]{"Todos", "VERDE", "AMARILLO", "ROJO"});
        cmbFiltroRecurso = new JComboBox<>(new String[]{"Ninguno", "AGUA", "ALIMENTO", "MEDICINA", "ABRIGO", "HIGIENE"});

        JButton btnFiltrar = new JButton("Filtrar");
        JButton btnLimpiar = new JButton("Mostrar Todos");
        JButton btnNuevoAlbergue = new JButton("+ Nuevo Albergue (RF-01)");
        btnNuevoAlbergue.setBackground(new Color(40, 116, 166));
        btnNuevoAlbergue.setForeground(Color.WHITE);
        JButton btnValidarSphereRed = new JButton("Validar Sphere Red (RF-06)");

        panelSuperior.add(new JLabel("Distrito:"));
        panelSuperior.add(txtFiltroDistrito);
        panelSuperior.add(new JLabel("Cupos mín:"));
        panelSuperior.add(spnFiltroCupos);
        panelSuperior.add(new JLabel("Semáforo:"));
        panelSuperior.add(cmbFiltroNivel);
        panelSuperior.add(new JLabel("Sin stock de:"));
        panelSuperior.add(cmbFiltroRecurso);
        panelSuperior.add(btnFiltrar);
        panelSuperior.add(btnLimpiar);
        panelSuperior.add(btnNuevoAlbergue);
        panelSuperior.add(btnValidarSphereRed);

        btnFiltrar.addActionListener(e -> {
            filtroActivo = true;
            actualizarTablaAlbergues();
        });
        btnLimpiar.addActionListener(e -> {
            filtroActivo = false;
            txtFiltroDistrito.setText("");
            spnFiltroCupos.setValue(0);
            cmbFiltroNivel.setSelectedIndex(0);
            cmbFiltroRecurso.setSelectedIndex(0);
            actualizarTablaAlbergues();
        });
        btnNuevoAlbergue.addActionListener(e -> mostrarDialogoNuevoAlbergue());
        btnValidarSphereRed.addActionListener(e -> mostrarReporteSphereRed());

        panel.add(panelSuperior, BorderLayout.NORTH);

        // --- Tabla central del Panel Semáforo (RF-05) ---
        String[] columnas = {"ID", "Nombre", "Distrito", "Infraestructura", "Área (m²)", "Ocupación", "Cupos Libres", "% Ocupación", "Semáforo"};
        modeloTablaAlbergues = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaAlbergues = new JTable(modeloTablaAlbergues);
        tablaAlbergues.setRowHeight(26);
        tablaAlbergues.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaAlbergues.getColumnModel().getColumn(7).setCellRenderer(new BarraOcupacionRenderer());
        tablaAlbergues.getColumnModel().getColumn(8).setCellRenderer(new SemaforoRenderer());

        tablaAlbergues.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarDetalleAlbergueSeleccionado();
            }
        });

        // --- Panel inferior: Detalle de Albergue y Alertas (RF-06, RF-09) ---
        JPanel panelDetalle = new JPanel(new GridLayout(1, 3, 8, 8));
        panelDetalle.setPreferredSize(new Dimension(100, 220));

        modeloListaAlertas = new DefaultListModel<>();
        JList<String> listaAlertas = new JList<>(modeloListaAlertas);
        JScrollPane scrollAlertas = new JScrollPane(listaAlertas);
        scrollAlertas.setBorder(new TitledBorder("Alertas Activas (Ocupación / Sphere / Stock)"));

        modeloListaDamnificadosDetalle = new DefaultListModel<>();
        JList<String> listaDamnificados = new JList<>(modeloListaDamnificadosDetalle);
        JScrollPane scrollDamnificados = new JScrollPane(listaDamnificados);
        scrollDamnificados.setBorder(new TitledBorder("Damnificados Alojados (RF-09)"));

        modeloListaRecursosDetalle = new DefaultListModel<>();
        JList<String> listaRecursos = new JList<>(modeloListaRecursosDetalle);
        JScrollPane scrollRecursos = new JScrollPane(listaRecursos);
        scrollRecursos.setBorder(new TitledBorder("Inventario del Albergue (RF-09)"));

        panelDetalle.add(scrollAlertas);
        panelDetalle.add(scrollDamnificados);
        panelDetalle.add(scrollRecursos);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(tablaAlbergues), panelDetalle);
        split.setResizeWeight(0.6);
        panel.add(split, BorderLayout.CENTER);

        return panel;
    }

    private void mostrarDialogoNuevoAlbergue() {
        JTextField txtId = new JTextField();
        JTextField txtNombre = new JTextField();
        JTextField txtDistrito = new JTextField();
        JTextField txtTipo = new JTextField("Coliseo");
        JSpinner spnCapacidad = new JSpinner(new SpinnerNumberModel(50, 1, 100000, 1));
        JTextField txtArea = new JTextField("200.0");

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("ID del Albergue (ej. A3):"));
        form.add(txtId);
        form.add(new JLabel("Nombre del Albergue:"));
        form.add(txtNombre);
        form.add(new JLabel("Distrito:"));
        form.add(txtDistrito);
        form.add(new JLabel("Tipo de Infraestructura:"));
        form.add(txtTipo);
        form.add(new JLabel("Capacidad Máxima:"));
        form.add(spnCapacidad);
        form.add(new JLabel("Área total en m² (0 si no se conoce):"));
        form.add(txtArea);

        int res = JOptionPane.showConfirmDialog(this, form, "Registrar Nuevo Albergue (RF-01)",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            double area = Double.parseDouble(txtArea.getText().trim().replace(',', '.'));
            Albergue nuevo = new Albergue(
                    txtId.getText(),
                    txtNombre.getText(),
                    txtDistrito.getText(),
                    txtTipo.getText(),
                    (Integer) spnCapacidad.getValue(),
                    area
            );
            if (!albergueService.registrarAlbergue(nuevo)) {
                JOptionPane.showMessageDialog(this, "Ya existe un albergue con ese ID.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            refrescarTodo();
            List<Alerta> alertas = albergueService.validarMinimosSphere(nuevo);
            StringBuilder msg = new StringBuilder("Albergue registrado correctamente: " + nuevo.getNombre());
            if (!alertas.isEmpty()) {
                msg.append("\n\nAdvertencias Sphere detectadas:");
                for (Alerta a : alertas) {
                    msg.append("\n• ").append(a);
                }
            }
            JOptionPane.showMessageDialog(this, msg.toString(), "Albergue Registrado", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "El área debe ser un número decimal válido.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Datos Inválidos", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mostrarReporteSphereRed() {
        StringBuilder sb = new StringBuilder("=== VALIDACIÓN DE MÍNIMOS SPHERE EN LA RED (RF-06) ===\n\n");
        for (Albergue a : albergueService.getAlbergues()) {
            sb.append("• ").append(a.getNombre()).append(" [").append(a.getId()).append("]:\n");
            List<Alerta> alertas = albergueService.validarMinimosSphere(a);
            if (alertas.isEmpty()) {
                sb.append("   ✓ Cumple los mínimos Sphere evaluados (agua y espacio vital).\n");
            } else {
                for (Alerta al : alertas) {
                    sb.append("   ! ").append(al).append("\n");
                }
            }
            sb.append("\n");
        }
        JTextArea area = new JTextArea(sb.toString(), 18, 60);
        area.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Reporte Mínimos Sphere (RF-06)", JOptionPane.INFORMATION_MESSAGE);
    }

    // =========================================================================
    // PESTAÑA 2: GESTIÓN DE DAMNIFICADOS (RF-02, RF-08)
    // =========================================================================
    private JPanel crearPanelDamnificados() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 12, 12));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- Columna Izquierda: Registro de Ingreso (RF-02) ---
        JPanel panelIngreso = new JPanel(new BorderLayout(8, 8));
        panelIngreso.setBorder(new TitledBorder("Registrar Ingreso de Damnificado (RF-02)"));

        JPanel formBasico = new JPanel(new GridLayout(0, 2, 6, 6));
        cmbAlbergueIngreso = new JComboBox<>();
        txtDniIngreso = new JTextField();
        txtNombreIngreso = new JTextField();
        spnEdadIngreso = new JSpinner(new SpinnerNumberModel(25, 0, Damnificado.EDAD_MAXIMA, 1));
        chkAtencionMedica = new JCheckBox("¿Requiere atención médica inmediata? (Prioridad CRÍTICA)");

        formBasico.add(new JLabel("Albergue Asignado:"));
        formBasico.add(cmbAlbergueIngreso);
        formBasico.add(new JLabel("DNI (8 dígitos):"));
        formBasico.add(txtDniIngreso);
        formBasico.add(new JLabel("Nombre Completo:"));
        formBasico.add(txtNombreIngreso);
        formBasico.add(new JLabel("Edad (0 - " + Damnificado.EDAD_MAXIMA + "):"));
        formBasico.add(spnEdadIngreso);
        formBasico.add(new JLabel("Asistencia Paramédica:"));
        formBasico.add(chkAtencionMedica);

        // CardLayout dinámico según edad (<18 Menor, >=18 Adulto General/Vulnerable)
        panelCardsPerfil = new JPanel(new CardLayout());

        // Card ADULTO
        JPanel cardAdulto = new JPanel(new GridLayout(0, 2, 6, 6));
        cardAdulto.setBorder(new TitledBorder("Perfil de Adulto (>= 18 años)"));
        cmbTipoAdulto = new JComboBox<>(new String[]{"General", "Vulnerable (Gestante, Crónico, Discapacidad)"});
        txtCondicionVulnerable = new JTextField();
        chkGestante = new JCheckBox("En estado de gestación");
        chkCronica = new JCheckBox("Enfermedad crónica o crítica");

        cardAdulto.add(new JLabel("Tipo de Perfil:"));
        cardAdulto.add(cmbTipoAdulto);
        cardAdulto.add(new JLabel("Condición Especial (opcional):"));
        cardAdulto.add(txtCondicionVulnerable);
        cardAdulto.add(chkGestante);
        cardAdulto.add(chkCronica);

        cmbTipoAdulto.addActionListener(e -> actualizarControlesVulnerable());
        actualizarControlesVulnerable();

        // Card MENOR
        JPanel cardMenor = new JPanel(new GridLayout(0, 2, 6, 6));
        cardMenor.setBorder(new TitledBorder("Datos de Menor de Edad (< 18 años)"));
        chkTieneTutor = new JCheckBox("Se encuentra acompañado por un tutor", true);
        txtDniTutor = new JTextField();
        txtNombreTutor = new JTextField();
        txtParentesco = new JTextField();

        cardMenor.add(new JLabel("Acompañamiento:"));
        cardMenor.add(chkTieneTutor);
        cardMenor.add(new JLabel("DNI del Tutor (8 dígitos):"));
        cardMenor.add(txtDniTutor);
        cardMenor.add(new JLabel("Nombre del Tutor:"));
        cardMenor.add(txtNombreTutor);
        cardMenor.add(new JLabel("Parentesco (ej. Madre, Tío):"));
        cardMenor.add(txtParentesco);

        chkTieneTutor.addActionListener(e -> {
            boolean conTutor = chkTieneTutor.isSelected();
            txtDniTutor.setEnabled(conTutor);
            txtNombreTutor.setEnabled(conTutor);
            txtParentesco.setEnabled(conTutor);
        });

        panelCardsPerfil.add(cardAdulto, "ADULTO");
        panelCardsPerfil.add(cardMenor, "MENOR");

        spnEdadIngreso.addChangeListener(e -> {
            int edad = (Integer) spnEdadIngreso.getValue();
            CardLayout cl = (CardLayout) panelCardsPerfil.getLayout();
            cl.show(panelCardsPerfil, edad < Damnificado.EDAD_MAYORIA ? "MENOR" : "ADULTO");
        });

        JButton btnRegistrarIngreso = new JButton("Registrar Ingreso al Albergue");
        btnRegistrarIngreso.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnRegistrarIngreso.addActionListener(e -> ejecutarRegistroIngreso());

        panelIngreso.add(formBasico, BorderLayout.NORTH);
        panelIngreso.add(panelCardsPerfil, BorderLayout.CENTER);
        panelIngreso.add(btnRegistrarIngreso, BorderLayout.SOUTH);

        // --- Columna Derecha: Derivación, Salida y Búsqueda (RF-08) ---
        JPanel panelDerecho = new JPanel(new GridLayout(3, 1, 8, 8));

        // 1. Derivación (RF-08)
        JPanel panelDerivar = new JPanel(new GridLayout(0, 2, 6, 6));
        panelDerivar.setBorder(new TitledBorder("Derivar / Reubicar Damnificado (RF-08)"));
        cmbAlbergueOrigen = new JComboBox<>();
        cmbAlbergueDestino = new JComboBox<>();
        txtDniDerivar = new JTextField();
        JButton btnDerivar = new JButton("Trasladar Damnificado");

        panelDerivar.add(new JLabel("Albergue de Origen:"));
        panelDerivar.add(cmbAlbergueOrigen);
        panelDerivar.add(new JLabel("DNI del Damnificado:"));
        panelDerivar.add(txtDniDerivar);
        panelDerivar.add(new JLabel("Albergue de Destino:"));
        panelDerivar.add(cmbAlbergueDestino);
        panelDerivar.add(new JLabel(""));
        panelDerivar.add(btnDerivar);
        btnDerivar.addActionListener(e -> ejecutarDerivacion());

        // 2. Salida Voluntaria (RF-08)
        JPanel panelSalida = new JPanel(new GridLayout(0, 2, 6, 6));
        panelSalida.setBorder(new TitledBorder("Registrar Salida Voluntaria (RF-08)"));
        cmbAlbergueSalida = new JComboBox<>();
        txtDniSalida = new JTextField();
        JButton btnSalida = new JButton("Registrar Salida y Liberar Cupo");

        panelSalida.add(new JLabel("Albergue:"));
        panelSalida.add(cmbAlbergueSalida);
        panelSalida.add(new JLabel("DNI del Damnificado:"));
        panelSalida.add(txtDniSalida);
        panelSalida.add(new JLabel(""));
        panelSalida.add(btnSalida);
        btnSalida.addActionListener(e -> ejecutarSalidaVoluntaria());

        // 3. Ubicar por DNI y seguimiento médico
        JPanel panelUbicar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 12));
        panelUbicar.setBorder(new TitledBorder("Ubicar Damnificado en la Red / Seguimiento Post-Atención"));
        JTextField txtDniBuscar = new JTextField(12);
        JButton btnBuscarDni = new JButton("Buscar por DNI");
        panelUbicar.add(new JLabel("DNI (8 dígitos):"));
        panelUbicar.add(txtDniBuscar);
        panelUbicar.add(btnBuscarDni);

        btnBuscarDni.addActionListener(e -> {
            String dni = txtDniBuscar.getText().trim();
            Optional<Albergue> optAlb = albergueService.ubicarDamnificado(dni);
            if (optAlb.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No se encontró ningún damnificado con el DNI " + dni + " en la red.");
                return;
            }
            Albergue alb = optAlb.get();
            Damnificado d = alb.buscarDamnificado(dni).orElseThrow();
            String info = "Alojado en: " + alb.getNombre() + " [" + alb.getId() + "]\n"
                    + "Persona: " + d + "\n"
                    + "Prioridad: " + d.determinarNivelPrioridad();
            if (d instanceof DamnificadoVulnerable v) {
                int op = JOptionPane.showConfirmDialog(this,
                        info + "\n\n¿Desea registrar verificación médica post-atención para esta persona vulnerable?",
                        "Damnificado Encontrado", JOptionPane.YES_NO_OPTION);
                if (op == JOptionPane.YES_OPTION) {
                    boolean fueAtendido = JOptionPane.showConfirmDialog(this, "¿Ya fue atendido por el personal?",
                            "Confirmación", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
                    boolean estaEstable = JOptionPane.showConfirmDialog(this, "¿Se encuentra estable tras la atención?",
                            "Estado Post-Atención", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
                    String msg1 = v.confirmacionAtencion(fueAtendido);
                    String msg2 = v.estadoPostAtencion(estaEstable);
                    JOptionPane.showMessageDialog(this, msg1 + "\n" + msg2);
                }
            } else {
                JOptionPane.showMessageDialog(this, info, "Damnificado Encontrado", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        panelDerecho.add(panelDerivar);
        panelDerecho.add(panelSalida);
        panelDerecho.add(panelUbicar);

        panel.add(panelIngreso);
        panel.add(panelDerecho);
        return panel;
    }

    private void actualizarControlesVulnerable() {
        boolean esVulnerable = cmbTipoAdulto.getSelectedIndex() == 1;
        txtCondicionVulnerable.setEnabled(esVulnerable);
        chkGestante.setEnabled(esVulnerable);
        chkCronica.setEnabled(esVulnerable);
    }

    private void ejecutarRegistroIngreso() {
        String idAlb = obtenerIdDeCombo(cmbAlbergueIngreso);
        if (idAlb == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un albergue válido.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String dni = txtDniIngreso.getText().trim();
        if (!Damnificado.esDniValido(dni)) {
            JOptionPane.showMessageDialog(this, "El DNI debe tener exactamente " + Damnificado.LONGITUD_DNI + " dígitos numéricos.",
                    "DNI Inválido", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String nombre = txtNombreIngreso.getText().trim();
        int edad = (Integer) spnEdadIngreso.getValue();
        boolean atencion = chkAtencionMedica.isSelected();

        try {
            Damnificado nuevo;
            if (edad < Damnificado.EDAD_MAYORIA) {
                String dniTutor = null;
                String nomTutor = null;
                String parentesco = null;
                if (chkTieneTutor.isSelected()) {
                    dniTutor = txtDniTutor.getText().trim();
                    if (!Damnificado.esDniValido(dniTutor)) {
                        JOptionPane.showMessageDialog(this, "El DNI del tutor debe tener 8 dígitos.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    nomTutor = txtNombreTutor.getText().trim();
                    parentesco = txtParentesco.getText().trim();
                    if (nomTutor.isEmpty() || parentesco.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Complete el nombre y parentesco del tutor.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                nuevo = new DamnificadoMenor(dni, nombre, edad, atencion, dniTutor, nomTutor, parentesco);
            } else if (cmbTipoAdulto.getSelectedIndex() == 1) {
                nuevo = new DamnificadoVulnerable(dni, nombre, edad, atencion,
                        txtCondicionVulnerable.getText(), chkGestante.isSelected(), chkCronica.isSelected());
            } else {
                nuevo = new DamnificadoGeneral(dni, nombre, edad, atencion);
            }

            ResultadoIngreso res = albergueService.registrarIngreso(idAlb, nuevo);
            switch (res) {
                case INGRESADO -> {
                    refrescarTodo();
                    limpiarFormularioIngreso();
                    JOptionPane.showMessageDialog(this,
                            "Ingreso registrado con éxito.\nPrioridad asignada: " + nuevo.determinarNivelPrioridad(),
                            "Ingreso Exitoso", JOptionPane.INFORMATION_MESSAGE);
                }
                case DNI_DUPLICADO -> JOptionPane.showMessageDialog(this,
                        "Ese DNI ya se encuentra alojado en la red de albergues.", "DNI Duplicado", JOptionPane.ERROR_MESSAGE);
                case ALBERGUE_NO_ENCONTRADO -> JOptionPane.showMessageDialog(this,
                        "Albergue no encontrado.", "Error", JOptionPane.ERROR_MESSAGE);
                case SIN_CUPO -> ofrecerDerivacionInteractiva(nuevo, idAlb);
            }
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Datos Inválidos", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void ofrecerDerivacionInteractiva(Damnificado damnificado, String idLleno) {
        List<Albergue> alternativas = albergueService.consultarDisponibilidad(idLleno);
        if (alternativas.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "El albergue seleccionado está lleno y NO hay otros albergues con cupo disponible en la red.",
                    "Sin Cupo en la Red", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String[] opciones = alternativas.stream()
                .map(a -> a.getId() + " - " + a.getNombre() + " (Libres: " + a.getCuposDisponibles() + ")")
                .toArray(String[]::new);

        String seleccion = (String) JOptionPane.showInputDialog(this,
                "El albergue " + idLleno + " no tiene cupo.\nSeleccione un albergue alternativo con disponibilidad (RF-08):",
                "Derivación por Falta de Cupo",
                JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);

        if (seleccion != null) {
            String idDestino = seleccion.split(" - ")[0].trim();
            if (albergueService.registrarIngreso(idDestino, damnificado) == ResultadoIngreso.INGRESADO) {
                refrescarTodo();
                limpiarFormularioIngreso();
                JOptionPane.showMessageDialog(this,
                        "Damnificado derivado y alojado exitosamente en " + seleccion + "\n" + damnificado.determinarNivelPrioridad());
            }
        }
    }

    private void limpiarFormularioIngreso() {
        txtDniIngreso.setText("");
        txtNombreIngreso.setText("");
        chkAtencionMedica.setSelected(false);
        txtCondicionVulnerable.setText("");
        chkGestante.setSelected(false);
        chkCronica.setSelected(false);
        txtDniTutor.setText("");
        txtNombreTutor.setText("");
        txtParentesco.setText("");
    }

    private void ejecutarDerivacion() {
        String idOrigen = obtenerIdDeCombo(cmbAlbergueOrigen);
        String idDestino = obtenerIdDeCombo(cmbAlbergueDestino);
        String dni = txtDniDerivar.getText().trim();
        if (!Damnificado.esDniValido(dni)) {
            JOptionPane.showMessageDialog(this, "Ingrese un DNI válido de 8 dígitos.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        ResultadoDerivacion r = albergueService.derivarDamnificado(idOrigen, idDestino, dni);
        switch (r) {
            case DERIVADO -> {
                refrescarTodo();
                txtDniDerivar.setText("");
                JOptionPane.showMessageDialog(this, "Damnificado trasladado con éxito a " + idDestino + ".");
            }
            case MISMO_ALBERGUE -> JOptionPane.showMessageDialog(this, "El origen y el destino no pueden ser el mismo albergue.", "Aviso", JOptionPane.WARNING_MESSAGE);
            case DAMNIFICADO_NO_ENCONTRADO -> JOptionPane.showMessageDialog(this, "Ese DNI no está alojado en el albergue de origen.", "Error", JOptionPane.ERROR_MESSAGE);
            case DESTINO_SIN_CUPO -> JOptionPane.showMessageDialog(this, "El albergue de destino no tiene cupos libres.", "Sin Cupo", JOptionPane.WARNING_MESSAGE);
            default -> JOptionPane.showMessageDialog(this, "No se pudo derivar: " + r, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void ejecutarSalidaVoluntaria() {
        String idAlb = obtenerIdDeCombo(cmbAlbergueSalida);
        String dni = txtDniSalida.getText().trim();
        if (!Damnificado.esDniValido(dni)) {
            JOptionPane.showMessageDialog(this, "Ingrese un DNI válido de 8 dígitos.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (albergueService.registrarSalida(idAlb, dni)) {
            refrescarTodo();
            txtDniSalida.setText("");
            JOptionPane.showMessageDialog(this, "Salida voluntaria registrada. El cupo fue liberado.");
        } else {
            JOptionPane.showMessageDialog(this, "Ese DNI no se encuentra alojado en el albergue seleccionado.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================================
    // PESTAÑA 3: INVENTARIO Y REPARTO SPHERE (RF-03, RF-04)
    // =========================================================================
    private JPanel crearPanelRecursos() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 12, 12));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- Columna Izquierda: Inventario (RF-03) ---
        JPanel panelInventario = new JPanel(new BorderLayout(8, 8));
        panelInventario.setBorder(new TitledBorder("Registrar y Actualizar Inventario de Recursos (RF-03)"));

        JPanel formRecurso = new JPanel(new GridLayout(0, 2, 6, 6));
        cmbAlbergueRecurso = new JComboBox<>();
        cmbTipoRecurso = new JComboBox<>(TipoRecurso.values());
        txtNombreRecurso = new JTextField("Agua potable");
        txtUnidadRecurso = new JTextField(AlbergueService.UNIDAD_AGUA);
        txtUnidadRecurso.setEditable(false);
        txtCantidadRecurso = new JTextField("100.0");
        txtVencimientoRecurso = new JTextField();

        cmbTipoRecurso.addActionListener(e -> {
            TipoRecurso tipo = (TipoRecurso) cmbTipoRecurso.getSelectedItem();
            if (tipo == TipoRecurso.AGUA) {
                txtUnidadRecurso.setText(AlbergueService.UNIDAD_AGUA);
                txtUnidadRecurso.setEditable(false);
            } else {
                txtUnidadRecurso.setEditable(true);
                if (txtUnidadRecurso.getText().equals(AlbergueService.UNIDAD_AGUA)) {
                    txtUnidadRecurso.setText("unidades");
                }
            }
        });

        formRecurso.add(new JLabel("Albergue:"));
        formRecurso.add(cmbAlbergueRecurso);
        formRecurso.add(new JLabel("Tipo de Recurso:"));
        formRecurso.add(cmbTipoRecurso);
        formRecurso.add(new JLabel("Nombre del Recurso:"));
        formRecurso.add(txtNombreRecurso);
        formRecurso.add(new JLabel("Unidad (Agua siempre en litros):"));
        formRecurso.add(txtUnidadRecurso);
        formRecurso.add(new JLabel("Cantidad Disponible:"));
        formRecurso.add(txtCantidadRecurso);
        formRecurso.add(new JLabel("Fecha Vencimiento (AAAA-MM-DD o vacío):"));
        formRecurso.add(txtVencimientoRecurso);

        JPanel botonesInventario = new JPanel(new GridLayout(2, 1, 6, 6));
        JButton btnRegistrarRecurso = new JButton("Registrar Lote en Inventario (RF-03)");
        JButton btnActualizarRecurso = new JButton("Modificar Stock / Vencimiento de Lote Existente...");
        botonesInventario.add(btnRegistrarRecurso);
        botonesInventario.add(btnActualizarRecurso);

        btnRegistrarRecurso.addActionListener(e -> ejecutarRegistroRecurso());
        btnActualizarRecurso.addActionListener(e -> mostrarDialogoActualizarRecurso());

        panelInventario.add(formRecurso, BorderLayout.NORTH);
        panelInventario.add(botonesInventario, BorderLayout.SOUTH);

        // --- Columna Derecha: Asignación por Prioridad (RF-04) ---
        JPanel panelAsignar = new JPanel(new BorderLayout(8, 8));
        panelAsignar.setBorder(new TitledBorder("Asignar Recurso Escaso por Orden de Prioridad (RF-04)"));

        JPanel formAsignar = new JPanel(new GridLayout(0, 2, 6, 6));
        cmbAlbergueAsignar = new JComboBox<>();
        cmbRecursoAsignar = new JComboBox<>();
        txtCantTotalAsignar = new JTextField("30.0");
        txtCantPorPersona = new JTextField("15.0");

        cmbAlbergueAsignar.addActionListener(e -> actualizarComboRecursosDeAsignacion());
        cmbRecursoAsignar.addActionListener(e -> {
            Recurso r = (Recurso) cmbRecursoAsignar.getSelectedItem();
            if (r != null) {
                txtCantPorPersona.setText(String.valueOf(asignacionService.cantidadSugeridaPorPersona(r)));
            }
            actualizarVistaPreviaPrioridad();
        });

        formAsignar.add(new JLabel("Albergue:"));
        formAsignar.add(cmbAlbergueAsignar);
        formAsignar.add(new JLabel("Recurso a Distribuir:"));
        formAsignar.add(cmbRecursoAsignar);
        formAsignar.add(new JLabel("Cantidad Total Máxima a Repartir:"));
        formAsignar.add(txtCantTotalAsignar);
        formAsignar.add(new JLabel("Cantidad por Persona (Sphere Agua = 15 L):"));
        formAsignar.add(txtCantPorPersona);

        modeloColaPrioridad = new DefaultListModel<>();
        JList<String> listaCola = new JList<>(modeloColaPrioridad);
        JScrollPane scrollCola = new JScrollPane(listaCola);
        scrollCola.setBorder(new TitledBorder("Orden de Prioridad Calculado en este Albergue"));

        txtResultadoReparto = new JTextArea(5, 30);
        txtResultadoReparto.setEditable(false);
        JScrollPane scrollRes = new JScrollPane(txtResultadoReparto);
        scrollRes.setBorder(new TitledBorder("Resultado del Último Reparto"));

        JPanel centroAsignar = new JPanel(new GridLayout(2, 1, 6, 6));
        centroAsignar.add(scrollCola);
        centroAsignar.add(scrollRes);

        JButton btnRepartir = new JButton("Ejecutar Reparto Priorizado (RF-04)");
        btnRepartir.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnRepartir.addActionListener(e -> ejecutarReparto());

        panelAsignar.add(formAsignar, BorderLayout.NORTH);
        panelAsignar.add(centroAsignar, BorderLayout.CENTER);
        panelAsignar.add(btnRepartir, BorderLayout.SOUTH);

        panel.add(panelInventario);
        panel.add(panelAsignar);
        return panel;
    }

    private void ejecutarRegistroRecurso() {
        String idAlb = obtenerIdDeCombo(cmbAlbergueRecurso);
        if (idAlb == null) {
            return;
        }
        try {
            TipoRecurso tipo = (TipoRecurso) cmbTipoRecurso.getSelectedItem();
            String nombre = txtNombreRecurso.getText().trim();
            String unidad = txtUnidadRecurso.getText().trim();
            double cantidad = Double.parseDouble(txtCantidadRecurso.getText().trim().replace(',', '.'));
            String textoFecha = txtVencimientoRecurso.getText().trim();
            LocalDate fecha = textoFecha.isEmpty() ? null : LocalDate.parse(textoFecha);

            Recurso nuevo = new Recurso(tipo, nombre, unidad, cantidad, fecha);
            if (albergueService.registrarRecurso(idAlb, nuevo)) {
                refrescarTodo();
                JOptionPane.showMessageDialog(this, "Recurso registrado correctamente en el albergue " + idAlb + ".");
            }
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Formato de fecha inválido. Use AAAA-MM-DD (ej. 2027-05-20) o déjelo vacío.",
                    "Fecha Inválida", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Ingrese una cantidad numérica válida.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de Validación", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mostrarDialogoActualizarRecurso() {
        String idAlb = obtenerIdDeCombo(cmbAlbergueRecurso);
        if (idAlb == null) {
            return;
        }
        Optional<Albergue> opt = albergueService.buscarPorId(idAlb);
        if (opt.isEmpty() || opt.get().getRecursos().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Este albergue no tiene recursos registrados para actualizar.");
            return;
        }
        Albergue alb = opt.get();
        JComboBox<Recurso> cmbRec = new JComboBox<>(alb.getRecursos().toArray(new Recurso[0]));
        JTextField txtNuevaCant = new JTextField();
        JTextField txtNuevaFecha = new JTextField();

        Runnable cargarValores = () -> {
            Recurso sel = (Recurso) cmbRec.getSelectedItem();
            if (sel != null) {
                txtNuevaCant.setText(String.valueOf(sel.getCantidadDisponible()));
                txtNuevaFecha.setText(sel.getFechaVencimiento() == null ? "" : sel.getFechaVencimiento().toString());
            }
        };
        cmbRec.addActionListener(e -> cargarValores.run());
        cargarValores.run();

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Lote a modificar:"));
        form.add(cmbRec);
        form.add(new JLabel("Nueva Cantidad Disponible:"));
        form.add(txtNuevaCant);
        form.add(new JLabel("Nueva Fecha (AAAA-MM-DD o vacío):"));
        form.add(txtNuevaFecha);

        if (JOptionPane.showConfirmDialog(this, form, "Actualizar Inventario en " + alb.getNombre(),
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                Recurso r = (Recurso) cmbRec.getSelectedItem();
                double cant = Double.parseDouble(txtNuevaCant.getText().trim().replace(',', '.'));
                String fStr = txtNuevaFecha.getText().trim();
                LocalDate f = fStr.isEmpty() ? null : LocalDate.parse(fStr);
                if (albergueService.actualizarRecurso(alb.getId(), r, cant, f)) {
                    refrescarTodo();
                    JOptionPane.showMessageDialog(this, "Inventario actualizado: " + r);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al actualizar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void ejecutarReparto() {
        String idAlb = obtenerIdDeCombo(cmbAlbergueAsignar);
        if (idAlb == null) {
            return;
        }
        Optional<Albergue> optAlb = albergueService.buscarPorId(idAlb);
        Recurso rec = (Recurso) cmbRecursoAsignar.getSelectedItem();
        if (optAlb.isEmpty() || rec == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un albergue y un recurso válido.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            double total = Double.parseDouble(txtCantTotalAsignar.getText().trim().replace(',', '.'));
            double porPersona = Double.parseDouble(txtCantPorPersona.getText().trim().replace(',', '.'));

            List<RegistroAsignacion> entregas = asignacionService.asignar(optAlb.get(), rec, total, porPersona);
            StringBuilder sb = new StringBuilder("Se realizaron " + entregas.size() + " entrega(s):\n");
            for (RegistroAsignacion reg : entregas) {
                sb.append("-> ").append(reg).append("\n");
            }
            if (rec.getTipo() == TipoRecurso.MEDICINA && entregas.isEmpty()) {
                sb.append("(Nota: La medicina solo se entrega a personas con atención médica, gestantes o crónicas).");
            }
            txtResultadoReparto.setText(sb.toString());
            refrescarTodo();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Las cantidades deben ser números válidos mayores que cero.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "No se pudo repartir", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================================
    // PESTAÑA 4: HISTORIAL Y PERSISTENCIA JSON
    // =========================================================================
    private JPanel crearPanelHistorialYJson() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        String[] cols = {"Fecha y Hora", "Albergue", "DNI", "Damnificado", "Recurso", "Cantidad", "Unidad"};
        modeloTablaHistorial = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable tablaHistorial = new JTable(modeloTablaHistorial);
        tablaHistorial.setRowHeight(24);
        JScrollPane scrollHist = new JScrollPane(tablaHistorial);
        scrollHist.setBorder(new TitledBorder("Historial General de Asignaciones de Recursos"));

        JPanel panelBotonesJson = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        panelBotonesJson.setBorder(new TitledBorder("Persistencia de Datos en Formato JSON"));

        JButton btnGuardarDefecto = new JButton("Guardar en datos_shelter.json");
        JButton btnGuardarComo = new JButton("Exportar JSON en otra ruta...");
        JButton btnCargarArchivo = new JButton("Cargar desde archivo JSON...");
        lblEstadoArchivo = new JLabel("Archivo predeterminado: " + RUTA_DATOS.toAbsolutePath());

        btnGuardarDefecto.addActionListener(e -> guardarConMensaje(RUTA_DATOS));
        btnGuardarComo.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(".");
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                guardarConMensaje(fc.getSelectedFile().toPath());
            }
        });
        btnCargarArchivo.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(".");
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                cargarConMensaje(fc.getSelectedFile().toPath());
            }
        });

        panelBotonesJson.add(btnGuardarDefecto);
        panelBotonesJson.add(btnGuardarComo);
        panelBotonesJson.add(btnCargarArchivo);
        panelBotonesJson.add(lblEstadoArchivo);

        panel.add(scrollHist, BorderLayout.CENTER);
        panel.add(panelBotonesJson, BorderLayout.SOUTH);
        return panel;
    }

    private void guardarConMensaje(Path ruta) {
        try {
            PersistenciaJson.guardar(ruta, albergueService.getAlbergues(), asignacionService.getHistorial());
            lblEstadoArchivo.setText("Último guardado: " + ruta.toAbsolutePath());
            JOptionPane.showMessageDialog(this, "Estado guardado correctamente en:\n" + ruta.toAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar: " + ex.getMessage(), "Error E/S", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarConMensaje(Path ruta) {
        try {
            PersistenciaJson.Datos datos = PersistenciaJson.cargar(ruta);
            albergueService.reemplazarAlbergues(datos.albergues());
            asignacionService.reemplazarHistorial(datos.historial());
            refrescarTodo();
            lblEstadoArchivo.setText("Cargado desde: " + ruta.toAbsolutePath());
            JOptionPane.showMessageDialog(this, "Datos cargados con éxito:\n• "
                    + datos.albergues().size() + " albergue(s)\n• " + datos.historial().size() + " entrega(s) en el historial.");
        } catch (IOException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "Archivo JSON inválido (no se alteraron los datos actuales):\n" + ex.getMessage(),
                    "Error al Cargar", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================================
    // SINCRONIZACIÓN Y REFRESCO DE TABLAS / COMBOS
    // =========================================================================
    private void refrescarTodo() {
        actualizarTablaAlbergues();
        actualizarCombosAlbergues();
        actualizarComboRecursosDeAsignacion();
        actualizarTablaHistorial();
    }

    private void actualizarTablaAlbergues() {
        String idSeleccionado = obtenerIdSeleccionadoEnTabla();
        modeloTablaAlbergues.setRowCount(0);

        List<Albergue> lista;
        if (filtroActivo) {
            int cuposMin = (Integer) spnFiltroCupos.getValue();
            Integer cupos = cuposMin > 0 ? cuposMin : null;
            String dist = txtFiltroDistrito.getText().trim();
            NivelSemaforo niv = cmbFiltroNivel.getSelectedIndex() > 0
                    ? NivelSemaforo.valueOf((String) cmbFiltroNivel.getSelectedItem()) : null;
            TipoRecurso rec = cmbFiltroRecurso.getSelectedIndex() > 0
                    ? TipoRecurso.valueOf((String) cmbFiltroRecurso.getSelectedItem()) : null;
            lista = albergueService.filtrarAlbergues(new FiltroAlbergue(cupos, dist, niv, rec));
        } else {
            lista = albergueService.obtenerPanelSemaforo();
        }

        int filaRestaurar = -1;
        for (int i = 0; i < lista.size(); i++) {
            Albergue a = lista.get(i);
            modeloTablaAlbergues.addRow(new Object[]{
                    a.getId(),
                    a.getNombre(),
                    a.getDistrito(),
                    a.getTipoInfraestructura(),
                    a.getAreaM2() > 0 ? String.format("%.1f", a.getAreaM2()) : "No reg.",
                    a.getOcupacionActual() + " / " + a.getCapacidadMaxima(),
                    a.getCuposDisponibles(),
                    (int) Math.round(a.getPorcentajeOcupacion()),
                    a.getEstadoSemaforo()
            });
            if (a.getId().equalsIgnoreCase(idSeleccionado)) {
                filaRestaurar = i;
            }
        }

        if (filaRestaurar >= 0) {
            tablaAlbergues.setRowSelectionInterval(filaRestaurar, filaRestaurar);
        } else if (tablaAlbergues.getRowCount() > 0) {
            tablaAlbergues.setRowSelectionInterval(0, 0);
        } else {
            mostrarDetalleAlbergueSeleccionado();
        }
    }

    private void mostrarDetalleAlbergueSeleccionado() {
        modeloListaAlertas.clear();
        modeloListaDamnificadosDetalle.clear();
        modeloListaRecursosDetalle.clear();

        String id = obtenerIdSeleccionadoEnTabla();
        if (id == null) {
            return;
        }
        Optional<Albergue> opt = albergueService.consultarAlbergue(id);
        if (opt.isEmpty()) {
            return;
        }
        Albergue a = opt.get();
        List<Alerta> alertas = albergueService.obtenerAlertas(a);
        if (alertas.isEmpty()) {
            modeloListaAlertas.addElement("[VERDE] Sin alertas activas. Cumple mínimos Sphere.");
        } else {
            alertas.forEach(al -> modeloListaAlertas.addElement(al.toString()));
        }
        for (Damnificado d : a.getDamnificados()) {
            modeloListaDamnificadosDetalle.addElement(d + " — [" + d.getNivelPrioridad().getEtiqueta() + "]");
        }
        for (Recurso r : a.getRecursos()) {
            modeloListaRecursosDetalle.addElement(r.toString());
        }
    }

    private String obtenerIdSeleccionadoEnTabla() {
        int fila = tablaAlbergues != null ? tablaAlbergues.getSelectedRow() : -1;
        return fila >= 0 ? (String) modeloTablaAlbergues.getValueAt(fila, 0) : null;
    }

    private void actualizarCombosAlbergues() {
        List<Albergue> red = albergueService.getAlbergues();
        recargarComboAlbergue(cmbAlbergueIngreso, red);
        recargarComboAlbergue(cmbAlbergueOrigen, red);
        recargarComboAlbergue(cmbAlbergueDestino, red);
        recargarComboAlbergue(cmbAlbergueSalida, red);
        recargarComboAlbergue(cmbAlbergueRecurso, red);
        recargarComboAlbergue(cmbAlbergueAsignar, red);
    }

    private void recargarComboAlbergue(JComboBox<String> combo, List<Albergue> albergues) {
        if (combo == null) {
            return;
        }
        String prev = obtenerIdDeCombo(combo);
        combo.removeAllItems();
        int idxSel = -1;
        for (int i = 0; i < albergues.size(); i++) {
            Albergue a = albergues.get(i);
            combo.addItem(a.getId() + " - " + a.getNombre() + " (Cupos: " + a.getCuposDisponibles() + ")");
            if (a.getId().equalsIgnoreCase(prev)) {
                idxSel = i;
            }
        }
        if (idxSel >= 0) {
            combo.setSelectedIndex(idxSel);
        }
    }

    private String obtenerIdDeCombo(JComboBox<String> combo) {
        Object item = combo != null ? combo.getSelectedItem() : null;
        if (item == null) {
            return null;
        }
        return item.toString().split(" - ")[0].trim();
    }

    private void actualizarComboRecursosDeAsignacion() {
        if (cmbRecursoAsignar == null) {
            return;
        }
        cmbRecursoAsignar.removeAllItems();
        String idAlb = obtenerIdDeCombo(cmbAlbergueAsignar);
        if (idAlb != null) {
            albergueService.buscarPorId(idAlb).ifPresent(a -> {
                for (Recurso r : a.getRecursos()) {
                    cmbRecursoAsignar.addItem(r);
                }
            });
        }
        actualizarVistaPreviaPrioridad();
    }

    private void actualizarVistaPreviaPrioridad() {
        if (modeloColaPrioridad == null) {
            return;
        }
        modeloColaPrioridad.clear();
        String idAlb = obtenerIdDeCombo(cmbAlbergueAsignar);
        if (idAlb == null) {
            return;
        }
        albergueService.buscarPorId(idAlb).ifPresent(alb -> {
            Recurso rec = (Recurso) cmbRecursoAsignar.getSelectedItem();
            List<Damnificado> ordenados = asignacionService.calcularOrdenPrioridad(alb, rec);
            int pos = 1;
            for (Damnificado d : ordenados) {
                modeloColaPrioridad.addElement((pos++) + ". [" + d.getNivelPrioridad().getEtiqueta() + "] " + d);
            }
        });
    }

    private void actualizarTablaHistorial() {
        if (modeloTablaHistorial == null) {
            return;
        }
        modeloTablaHistorial.setRowCount(0);
        for (RegistroAsignacion r : asignacionService.getHistorial()) {
            modeloTablaHistorial.addRow(new Object[]{
                    r.getFechaHora().format(FMT_FECHA_HORA),
                    r.getIdAlbergue(),
                    r.getDni(),
                    r.getNombreDamnificado(),
                    r.getRecurso(),
                    String.format("%.2f", r.getCantidad()),
                    r.getUnidad()
            });
        }
    }

    // =========================================================================
    // RENDERERS PERSONALIZADOS (Barra de Progreso y Semáforo RNF-01)
    // =========================================================================
    private static class BarraOcupacionRenderer extends JProgressBar implements TableCellRenderer {
        BarraOcupacionRenderer() {
            super(0, 100);
            setStringPainted(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            int pct = (value instanceof Integer i) ? i : 0;
            setValue(pct);
            setString(pct + "%");
            return this;
        }
    }

    private static class SemaforoRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
            if (value instanceof NivelSemaforo nivel) {
                // RNF-01: Mostrar tanto el color como la etiqueta en texto
                lbl.setText(nivel.getEtiqueta());
                switch (nivel) {
                    case VERDE -> {
                        lbl.setBackground(new Color(212, 239, 223));
                        lbl.setForeground(new Color(20, 90, 50));
                    }
                    case AMARILLO -> {
                        lbl.setBackground(new Color(252, 243, 207));
                        lbl.setForeground(new Color(125, 102, 8));
                    }
                    case ROJO -> {
                        lbl.setBackground(new Color(245, 183, 177));
                        lbl.setForeground(new Color(120, 40, 31));
                    }
                }
            }
            return lbl;
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> new VentanaPrincipal().setVisible(true));
    }
}