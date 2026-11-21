package com.shelter.ui.vistas;

import com.shelter.controller.ShelterController;
import com.shelter.model.Albergue;
import com.shelter.model.Damnificado;
import com.shelter.model.DamnificadoGeneral;
import com.shelter.model.DamnificadoMenor;
import com.shelter.model.DamnificadoVulnerable;
import com.shelter.model.NivelPrioridad;
import com.shelter.service.AlbergueService.ResultadoDerivacion;
import com.shelter.service.AlbergueService.ResultadoIngreso;
import com.shelter.ui.componentes.BotonModerno;
import com.shelter.ui.componentes.Formularios;
import com.shelter.ui.componentes.Iconos;
import com.shelter.ui.componentes.Renderizadores;
import com.shelter.ui.componentes.Tarjeta;
import com.shelter.ui.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Gestión de damnificados: registro de ingreso con vista previa de prioridad (RF-02), derivación,
 * salida voluntaria (RF-08) y ubicación por DNI con verificación post-atención.
 *
 * Mejora de consistencia: derivar y dar salida ya no exige escribir el código a mano; se elige a la
 * persona de una lista del albergue de origen, así no puede haber errores de tipeo.
 */
public class PanelDamnificados extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final Color COLOR_PERSONAS = new Color(0x2DD4BF);

    private final transient ShelterController controller;
    private boolean refrescando;

    // Ingreso
    private final JComboBox<Albergue> cmbAlbergueIngreso = comboAlbergues();
    private final JTextField txtDni = Formularios.campo("");
    private final JTextField txtNombre = Formularios.campo("");
    private final JSpinner spnEdad = new JSpinner(new SpinnerNumberModel(25, 0, Damnificado.EDAD_MAXIMA, 1));
    private final JCheckBox chkAtencion = Formularios.casilla("Requiere atención médica inmediata");
    private final CardLayout cartasPerfil = new CardLayout();
    private final JPanel panelPerfil = new JPanel(cartasPerfil);
    private final JComboBox<String> cmbTipoAdulto = new JComboBox<>(new String[]{"General", "Vulnerable"});
    private final JTextField txtCondicion = Formularios.campo("");
    private final JCheckBox chkGestante = Formularios.casilla("Gestante");
    private final JCheckBox chkCronica = Formularios.casilla("Enfermedad crónica");
    private final JCheckBox chkTutor = Formularios.casilla("Acompañado por un tutor");
    private final JTextField txtDniTutor = Formularios.campo("");
    private final JTextField txtNombreTutor = Formularios.campo("");
    private final JTextField txtParentesco = Formularios.campo("");
    private final Renderizadores.Chip chipPrioridad = new Renderizadores.Chip();
    private final javax.swing.JLabel lblDescripcion = Formularios.nota(" ");

    // Derivación
    private final JComboBox<Albergue> cmbOrigen = comboAlbergues();
    private final JComboBox<Damnificado> cmbPersonaDerivar = comboPersonas();
    private final JComboBox<Albergue> cmbDestino = comboAlbergues();

    // Salida
    private final JComboBox<Albergue> cmbAlbergueSalida = comboAlbergues();
    private final JComboBox<Damnificado> cmbPersonaSalida = comboPersonas();

    // Ubicar
    private final JTextField txtDniBuscar = Formularios.campo("");

    public PanelDamnificados(ShelterController controller) {
        super(new GridLayout(1, 2, 16, 0));
        this.controller = controller;
        setOpaque(false);
        add(construirIngreso());
        add(construirAcciones());
        actualizarPerfil();
    }

    private static JComboBox<Albergue> comboAlbergues() {
        JComboBox<Albergue> c = new JComboBox<>();
        c.setRenderer(new Renderizadores.AlbergueCombo());
        return c;
    }

    private static JComboBox<Damnificado> comboPersonas() {
        JComboBox<Damnificado> c = new JComboBox<>();
        c.setRenderer(new Renderizadores.DamnificadoCombo());
        return c;
    }

    // =========================================================
    // INGRESO (RF-02)
    // =========================================================

    private Tarjeta construirIngreso() {
        Formularios.Rejilla basico = new Formularios.Rejilla()
                .fila("Albergue", cmbAlbergueIngreso)
                .fila("DNI (8 dígitos)", txtDni)
                .fila("Nombre completo", txtNombre)
                .fila("Edad", spnEdad)
                .completa(chkAtencion);

        Formularios.Rejilla adulto = new Formularios.Rejilla()
                .fila("Perfil", cmbTipoAdulto)
                .fila("Condición (opcional)", txtCondicion)
                .completa(filaCasillas(chkGestante, chkCronica))
                .empujar();
        Formularios.Rejilla menor = new Formularios.Rejilla()
                .completa(chkTutor)
                .fila("DNI del tutor", txtDniTutor)
                .fila("Nombre del tutor", txtNombreTutor)
                .fila("Parentesco", txtParentesco)
                .empujar();
        cmbTipoAdulto.setRenderer(new Renderizadores.TextoCombo());
        panelPerfil.setOpaque(false);
        panelPerfil.add(envolver("Perfil de adulto (18+)", adulto), "ADULTO");
        panelPerfil.add(envolver("Datos del menor (< 18)", menor), "MENOR");
        chkTutor.setSelected(true);

        // Vista previa de prioridad: se recalcula con las MISMAS reglas del modelo (polimorfismo)
        Runnable alCambiar = this::actualizarPerfil;
        spnEdad.addChangeListener(e -> alCambiar.run());
        for (JCheckBox c : new JCheckBox[]{chkAtencion, chkGestante, chkCronica, chkTutor}) {
            c.addActionListener(e -> alCambiar.run());
        }
        cmbTipoAdulto.addActionListener(e -> alCambiar.run());

        JPanel vistaPrevia = new JPanel(new BorderLayout(10, 4));
        vistaPrevia.setOpaque(false);
        vistaPrevia.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDE_SUAVE),
                BorderFactory.createEmptyBorder(10, 0, 0, 0)));
        vistaPrevia.add(Formularios.etiqueta("Prioridad estimada"), BorderLayout.WEST);
        vistaPrevia.add(chipPrioridad, BorderLayout.CENTER);
        vistaPrevia.add(lblDescripcion, BorderLayout.SOUTH);

        BotonModerno btnRegistrar = new BotonModerno("Registrar ingreso", BotonModerno.Estilo.PRIMARIO, new Color(0x0F766E));
        btnRegistrar.setIcon(Iconos.de(Iconos.Figura.PERSONAS, 16, Color.WHITE));
        btnRegistrar.addActionListener(e -> registrarIngreso());

        JPanel sur = new JPanel(new BorderLayout(0, 12));
        sur.setOpaque(false);
        sur.add(vistaPrevia, BorderLayout.CENTER);
        sur.add(btnRegistrar, BorderLayout.SOUTH);

        JPanel cuerpo = new JPanel(new BorderLayout(0, 10));
        cuerpo.setOpaque(false);
        cuerpo.add(basico, BorderLayout.NORTH);
        cuerpo.add(panelPerfil, BorderLayout.CENTER);
        cuerpo.add(sur, BorderLayout.SOUTH);
        return new Tarjeta("Registrar ingreso de damnificado (RF-02)", COLOR_PERSONAS).contenido(cuerpo);
    }

    private static JPanel filaCasillas(JComponent... casillas) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.setOpaque(false);
        for (JComponent c : casillas) {
            p.add(c);
            p.add(javax.swing.Box.createHorizontalStrut(16));
        }
        return p;
    }

    private static JPanel envolver(String titulo, JComponent contenido) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        javax.swing.JLabel l = new javax.swing.JLabel(titulo.toUpperCase());
        l.setFont(Tema.fuente(java.awt.Font.BOLD, 10));
        l.setForeground(Tema.TEXTO_TENUE);
        p.add(l, BorderLayout.NORTH);
        p.add(contenido, BorderLayout.CENTER);
        return p;
    }

    private boolean esMenor() {
        return (Integer) spnEdad.getValue() < Damnificado.EDAD_MAYORIA;
    }

    private void actualizarPerfil() {
        boolean menor = esMenor();
        cartasPerfil.show(panelPerfil, menor ? "MENOR" : "ADULTO");
        boolean vulnerable = cmbTipoAdulto.getSelectedIndex() == 1;
        txtCondicion.setEnabled(vulnerable);
        chkGestante.setEnabled(vulnerable);
        chkCronica.setEnabled(vulnerable);
        boolean tutor = chkTutor.isSelected();
        txtDniTutor.setEnabled(tutor);
        txtNombreTutor.setEnabled(tutor);
        txtParentesco.setEnabled(tutor);

        // Persona "de muestra" con los datos marcados: la prioridad sale del propio modelo
        Damnificado muestra = crearPersona("00000000", "Vista previa", tutor ? "00000000" : null, "", "");
        NivelPrioridad nivel = muestra.getNivelPrioridad();
        chipPrioridad.configurar(nivel.getEtiqueta(), Tema.color(nivel), true);
        String descripcion = muestra.determinarNivelPrioridad();
        lblDescripcion.setText("<html>" + Renderizadores.escapar(descripcion.substring(descripcion.indexOf('-') + 1).trim()) + "</html>");
    }

    /** Crea el subtipo correcto según la edad y el perfil marcado. */
    private Damnificado crearPersona(String dni, String nombre, String dniTutor, String nombreTutor, String parentesco) {
        int edad = (Integer) spnEdad.getValue();
        boolean atencion = chkAtencion.isSelected();
        if (edad < Damnificado.EDAD_MAYORIA) {
            return new DamnificadoMenor(dni, nombre, edad, atencion, dniTutor, nombreTutor, parentesco);
        }
        if (cmbTipoAdulto.getSelectedIndex() == 1) {
            return new DamnificadoVulnerable(dni, nombre, edad, atencion, txtCondicion.getText(),
                    chkGestante.isSelected(), chkCronica.isSelected());
        }
        return new DamnificadoGeneral(dni, nombre, edad, atencion);
    }

    private void registrarIngreso() {
        try {
            Albergue albergue = Formularios.albergue(cmbAlbergueIngreso);
            String dni = txtDni.getText().trim();
            if (!Damnificado.esDniValido(dni)) {
                txtDni.requestFocusInWindow();
                throw new Formularios.EntradaInvalida("El DNI debe tener exactamente " + Damnificado.LONGITUD_DNI + " dígitos.");
            }
            String nombre = Formularios.textoObligatorio(txtNombre, "El nombre");
            String dniTutor = null;
            String nombreTutor = null;
            String parentesco = null;
            if (esMenor() && chkTutor.isSelected()) {
                dniTutor = txtDniTutor.getText().trim();
                if (!Damnificado.esDniValido(dniTutor)) {
                    txtDniTutor.requestFocusInWindow();
                    throw new Formularios.EntradaInvalida("El DNI del tutor debe tener 8 dígitos.");
                }
                if (dniTutor.equals(dni)) {
                    throw new Formularios.EntradaInvalida("El DNI del tutor no puede ser el del propio menor.");
                }
                nombreTutor = Formularios.textoObligatorio(txtNombreTutor, "El nombre del tutor");
                parentesco = Formularios.textoObligatorio(txtParentesco, "El parentesco");
            }
            Damnificado nuevo = crearPersona(dni, nombre, dniTutor, nombreTutor, parentesco);
            ResultadoIngreso r = controller.registrarIngreso(albergue.getId(), nuevo);
            switch (r) {
                case INGRESADO -> confirmarIngreso(nuevo, albergue);
                case IDENTIDAD_DUPLICADA -> Formularios.error(this, "El DNI o el nombre ya está registrado en la red de albergues.");
                case ALBERGUE_NO_ENCONTRADO -> Formularios.error(this, "El albergue seleccionado ya no existe.");
                case SIN_CUPO -> ofrecerOtroAlbergue(nuevo, albergue);
            }
        } catch (Formularios.EntradaInvalida | IllegalArgumentException ex) {
            Formularios.error(this, ex.getMessage());
        }
    }

    private void confirmarIngreso(Damnificado d, Albergue albergue) {
        limpiarFormulario();
        Formularios.info(this, "Ingreso registrado", "Código asignado: " + d.getId()
                + "\nAlbergue: " + albergue.getNombre()
                + "\nPrioridad: " + d.determinarNivelPrioridad());
    }

    /** RF-08: el albergue está lleno; se ofrecen los que tienen cupo. */
    private void ofrecerOtroAlbergue(Damnificado d, Albergue lleno) {
        List<Albergue> alternativas = controller.alberguesConCupo(lleno.getId());
        if (alternativas.isEmpty()) {
            Formularios.aviso(this, lleno.getNombre() + " está lleno y no hay otros albergues con cupo en la red.");
            return;
        }
        String[] opciones = alternativas.stream()
                .map(a -> a.getId() + " · " + a.getNombre() + " (" + a.getCuposDisponibles() + " cupos)")
                .toArray(String[]::new);
        Object eleccion = JOptionPane.showInputDialog(this,
                lleno.getNombre() + " no tiene cupo.\nElija un albergue alternativo:",
                "Derivación por falta de cupo", JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);
        if (eleccion == null) {
            return;
        }
        Albergue destino = alternativas.get(java.util.Arrays.asList(opciones).indexOf(eleccion));
        ResultadoIngreso r = controller.registrarIngreso(destino.getId(), d);
        if (r == ResultadoIngreso.INGRESADO) {
            confirmarIngreso(d, destino);
        } else {
            Formularios.error(this, "No se pudo alojar en " + destino.getNombre() + ": " + r);
        }
    }

    private void limpiarFormulario() {
        for (JTextField t : new JTextField[]{txtDni, txtNombre, txtCondicion, txtDniTutor, txtNombreTutor, txtParentesco}) {
            t.setText("");
        }
        chkAtencion.setSelected(false);
        chkGestante.setSelected(false);
        chkCronica.setSelected(false);
        actualizarPerfil();
    }

    // =========================================================
    // DERIVACIÓN, SALIDA Y UBICACIÓN (RF-08)
    // =========================================================

    private JPanel construirAcciones() {
        JPanel columna = new JPanel(new GridBagLayout());
        columna.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 14, 0);

        // Derivar
        cmbOrigen.addActionListener(e -> {
            if (!refrescando) {
                recargarPersonas(cmbOrigen, cmbPersonaDerivar);
                recargarDestinos();
            }
        });
        BotonModerno btnDerivar = new BotonModerno("Trasladar");
        btnDerivar.setIcon(Iconos.de(Iconos.Figura.CAMION, 16, Color.WHITE));
        btnDerivar.addActionListener(e -> derivar());
        Formularios.Rejilla derivar = new Formularios.Rejilla()
                .fila("Albergue de origen", cmbOrigen)
                .fila("Persona", cmbPersonaDerivar)
                .fila("Albergue de destino", cmbDestino)
                .completa(alinearDerecha(btnDerivar));
        c.gridy = 0;
        c.weighty = 0;
        columna.add(new Tarjeta("Derivar / reubicar (RF-08)", Tema.ACENTO).contenido(derivar), c);

        // Salida
        cmbAlbergueSalida.addActionListener(e -> {
            if (!refrescando) {
                recargarPersonas(cmbAlbergueSalida, cmbPersonaSalida);
            }
        });
        BotonModerno btnSalida = new BotonModerno("Registrar salida", BotonModerno.Estilo.PELIGRO);
        btnSalida.setIcon(Iconos.de(Iconos.Figura.SALIDA, 16, Color.WHITE));
        btnSalida.addActionListener(e -> registrarSalida());
        Formularios.Rejilla salida = new Formularios.Rejilla()
                .fila("Albergue", cmbAlbergueSalida)
                .fila("Persona", cmbPersonaSalida)
                .completa(alinearDerecha(btnSalida));
        c.gridy = 1;
        columna.add(new Tarjeta("Salida voluntaria (RF-08)", Tema.ROJO).contenido(salida), c);

        // Ubicar por DNI
        BotonModerno btnBuscar = new BotonModerno("Buscar", BotonModerno.Estilo.SECUNDARIO);
        btnBuscar.setIcon(Iconos.de(Iconos.Figura.LUPA, 14, Tema.ACENTO));
        btnBuscar.addActionListener(e -> ubicarPorDni());
        txtDniBuscar.addActionListener(e -> ubicarPorDni());
        JPanel buscar = new JPanel(new BorderLayout(10, 0));
        buscar.setOpaque(false);
        buscar.add(txtDniBuscar, BorderLayout.CENTER);
        buscar.add(btnBuscar, BorderLayout.EAST);
        Formularios.Rejilla ubicar = new Formularios.Rejilla()
                .fila("DNI", buscar)
                .completa(Formularios.nota("Verificación secundaria: muestra dónde está alojada la persona y permite "
                        + "registrar la atención post-médica de personas vulnerables."));
        c.gridy = 2;
        c.weighty = 1;
        c.insets = new Insets(0, 0, 0, 0);
        columna.add(new Tarjeta("Ubicar en la red", new Color(0xC084FC)).contenido(ubicar), c);
        return columna;
    }

    private static JPanel alinearDerecha(JComponent c) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        p.setOpaque(false);
        p.add(c);
        return p;
    }

    private void recargarPersonas(JComboBox<Albergue> origen, JComboBox<Damnificado> personas) {
        Albergue a = (Albergue) origen.getSelectedItem();
        List<Damnificado> lista = a == null ? List.of() : new ArrayList<>(a.getDamnificados());
        Formularios.recargar(personas, lista, Damnificado::getId);
    }

    private void recargarDestinos() {
        Albergue origen = (Albergue) cmbOrigen.getSelectedItem();
        Formularios.recargar(cmbDestino, controller.alberguesConCupo(origen == null ? null : origen.getId()), Albergue::getId);
    }

    private void derivar() {
        try {
            Albergue origen = Formularios.albergue(cmbOrigen);
            Damnificado persona = (Damnificado) cmbPersonaDerivar.getSelectedItem();
            if (persona == null) {
                throw new Formularios.EntradaInvalida("El albergue de origen no tiene personas para trasladar.");
            }
            Albergue destino = (Albergue) cmbDestino.getSelectedItem();
            if (destino == null) {
                throw new Formularios.EntradaInvalida("No hay otro albergue con cupo disponible.");
            }
            String codigoAnterior = persona.getId();
            ResultadoDerivacion r = controller.derivar(origen.getId(), destino.getId(), codigoAnterior);
            switch (r) {
                case DERIVADO -> Formularios.info(this, "Traslado realizado", persona.getNombreCompleto()
                        + " fue trasladado/a a " + destino.getNombre()
                        + ".\nCódigo anterior: " + codigoAnterior + "   →   Código nuevo: " + persona.getId());
                case MISMO_ALBERGUE -> Formularios.aviso(this, "El origen y el destino no pueden ser el mismo albergue.");
                case DESTINO_SIN_CUPO -> Formularios.aviso(this, "El albergue de destino ya no tiene cupo.");
                default -> Formularios.error(this, "No se pudo derivar: " + r);
            }
        } catch (Formularios.EntradaInvalida ex) {
            Formularios.error(this, ex.getMessage());
        }
    }

    private void registrarSalida() {
        try {
            Albergue albergue = Formularios.albergue(cmbAlbergueSalida);
            Damnificado persona = (Damnificado) cmbPersonaSalida.getSelectedItem();
            if (persona == null) {
                throw new Formularios.EntradaInvalida("Ese albergue no tiene personas alojadas.");
            }
            if (!Formularios.confirmar(this, "Confirmar salida", "¿Registrar la salida de " + persona.getNombreCompleto()
                    + " (" + persona.getId() + ")?\nSe liberará su cupo en " + albergue.getNombre() + ".")) {
                return;
            }
            if (controller.registrarSalida(albergue.getId(), persona.getId())) {
                Formularios.info(this, "Salida registrada", "Salida registrada. Cupo liberado en " + albergue.getNombre() + ".");
            } else {
                Formularios.error(this, "Esa persona ya no está alojada en el albergue seleccionado.");
            }
        } catch (Formularios.EntradaInvalida ex) {
            Formularios.error(this, ex.getMessage());
        }
    }

    private void ubicarPorDni() {
        String dni = txtDniBuscar.getText().trim();
        if (!Damnificado.esDniValido(dni)) {
            Formularios.error(this, "Ingrese un DNI de 8 dígitos.");
            return;
        }
        Optional<Albergue> encontrado = controller.ubicarPorDni(dni);
        if (encontrado.isEmpty()) {
            Formularios.info(this, "Sin resultados", "No hay ningún damnificado con DNI " + dni + " en la red.");
            return;
        }
        Albergue albergue = encontrado.get();
        Damnificado d = albergue.buscarDamnificado(dni).orElseThrow();
        String info = "Alojado en: " + albergue.getNombre() + " [" + albergue.getId() + "]\n"
                + "Persona: " + d + "\n"
                + "Prioridad: " + d.determinarNivelPrioridad();
        if (d instanceof DamnificadoVulnerable v) {
            info += "\nAtendido: " + (v.isAtendido() ? "sí" : "no") + " · Estable: " + (v.isEstable() ? "sí" : "no");
            if (Formularios.confirmar(this, "Damnificado encontrado", info + "\n\n¿Registrar verificación post-atención?")) {
                boolean atendido = Formularios.confirmar(this, "Verificación", "¿Ya fue atendido/a por el personal?");
                boolean estable = Formularios.confirmar(this, "Estado post-atención", "¿Se encuentra estable tras la atención?");
                controller.registrarVerificacion(v, atendido, estable);
                Formularios.info(this, "Verificación registrada",
                        (atendido ? "Atendido/a" : "Aún no atendido/a") + " · " + (estable ? "estable" : "en recuperación"));
            }
        } else {
            Formularios.info(this, "Damnificado encontrado", info);
        }
    }

    // =========================================================
    // REFRESCO
    // =========================================================

    public void refrescar() {
        refrescando = true;
        try {
            List<Albergue> red = controller.getAlbergues();
            Formularios.recargar(cmbAlbergueIngreso, red, Albergue::getId);
            Formularios.recargar(cmbOrigen, red, Albergue::getId);
            Formularios.recargar(cmbAlbergueSalida, red, Albergue::getId);
            recargarPersonas(cmbOrigen, cmbPersonaDerivar);
            recargarPersonas(cmbAlbergueSalida, cmbPersonaSalida);
            recargarDestinos();
        } finally {
            refrescando = false;
        }
        repaint();
    }
}
