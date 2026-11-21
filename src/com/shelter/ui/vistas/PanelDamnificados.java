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
 * Gestión de damnificados: ingreso con vista previa de prioridad (RF-02), derivación, salida
 * voluntaria (RF-08) y ubicación por DNI con verificación post-atención.
 *
 * - Derivar y dar salida se hace eligiendo a la persona de una lista (sin escribir códigos).
 * - Los resultados y errores aparecen en el propio formulario, no en ventanas modales.
 * - Enter en un campo envía su formulario.
 * - Los combos de albergue siguen al albergue en foco de toda la aplicación.
 */
public class PanelDamnificados extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient ShelterController controller;
    private final transient Contexto contexto;
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
    private final Formularios.AvisoEnLinea avisoIngreso = new Formularios.AvisoEnLinea();

    // Derivación
    private final JComboBox<Albergue> cmbOrigen = comboAlbergues();
    private final JComboBox<Damnificado> cmbPersonaDerivar = comboPersonas();
    private final JComboBox<Albergue> cmbDestino = comboAlbergues();
    private final Formularios.AvisoEnLinea avisoDerivar = new Formularios.AvisoEnLinea();
    private BotonModerno btnDerivar;

    // Salida
    private final JComboBox<Albergue> cmbAlbergueSalida = comboAlbergues();
    private final JComboBox<Damnificado> cmbPersonaSalida = comboPersonas();
    private final Formularios.AvisoEnLinea avisoSalida = new Formularios.AvisoEnLinea();

    // Ubicar
    private final JTextField txtDniBuscar = Formularios.campo("");
    private final Formularios.AvisoEnLinea avisoUbicar = new Formularios.AvisoEnLinea();

    public PanelDamnificados(ShelterController controller, Contexto contexto) {
        super(new GridLayout(1, 2, 16, 0));
        this.controller = controller;
        this.contexto = contexto;
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
        cmbAlbergueIngreso.addActionListener(e -> recordarAlbergue(cmbAlbergueIngreso));
        Formularios.Rejilla basico = new Formularios.Rejilla()
                .fila("Albergue", cmbAlbergueIngreso)
                .fila("DNI (8 dígitos)", txtDni)
                .fila("Nombre completo", txtNombre)
                .fila("Edad", spnEdad)
                .completa(chkAtencion);

        cmbTipoAdulto.setRenderer(new Renderizadores.TextoCombo());
        Formularios.Rejilla adulto = new Formularios.Rejilla()
                .fila("Perfil", cmbTipoAdulto)
                .fila("Condición (opcional)", txtCondicion)
                .completa(filaCasillas(chkGestante, chkCronica));
        Formularios.Rejilla menor = new Formularios.Rejilla()
                .completa(chkTutor)
                .fila("DNI del tutor", txtDniTutor)
                .fila("Nombre del tutor", txtNombreTutor)
                .fila("Parentesco", txtParentesco);
        panelPerfil.setOpaque(false);
        panelPerfil.add(envolver("Perfil de adulto (18 años o más)", adulto), "ADULTO");
        panelPerfil.add(envolver("Datos del menor (menos de 18 años)", menor), "MENOR");
        chkTutor.setSelected(true);

        // Vista previa de prioridad: se recalcula con las MISMAS reglas del modelo (polimorfismo)
        spnEdad.addChangeListener(e -> actualizarPerfil());
        for (JCheckBox c : new JCheckBox[]{chkAtencion, chkGestante, chkCronica, chkTutor}) {
            c.addActionListener(e -> actualizarPerfil());
        }
        cmbTipoAdulto.addActionListener(e -> actualizarPerfil());

        JPanel vistaPrevia = new JPanel(new BorderLayout(10, 4));
        vistaPrevia.setOpaque(false);
        vistaPrevia.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDE_SUAVE),
                BorderFactory.createEmptyBorder(12, 0, 0, 0)));
        vistaPrevia.add(Formularios.etiqueta("Prioridad estimada"), BorderLayout.WEST);
        vistaPrevia.add(chipPrioridad, BorderLayout.CENTER);
        vistaPrevia.add(lblDescripcion, BorderLayout.SOUTH);

        BotonModerno btnRegistrar = new BotonModerno("Registrar ingreso", BotonModerno.Estilo.PRIMARIO);
        btnRegistrar.setIcon(Iconos.de(Iconos.Figura.PERSONAS, 16, Tema.TINTA_ACCION));
        btnRegistrar.setToolTipText("Enter en cualquier campo también registra");
        btnRegistrar.addActionListener(e -> registrarIngreso());
        // Enter en cualquier campo de texto del formulario registra el ingreso
        for (JTextField t : new JTextField[]{txtDni, txtNombre, txtCondicion, txtDniTutor, txtNombreTutor, txtParentesco}) {
            t.addActionListener(e -> registrarIngreso());
        }

        // Todo en una columna continua: la vista previa queda junto a los campos que la cambian
        JPanel cuerpo = new JPanel(new GridBagLayout());
        cuerpo.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 10, 0);
        c.gridy = 0;
        cuerpo.add(basico, c);
        c.gridy = 1;
        cuerpo.add(panelPerfil, c);
        c.gridy = 2;
        cuerpo.add(vistaPrevia, c);
        c.gridy = 3;
        cuerpo.add(btnRegistrar, c);
        c.gridy = 4;
        cuerpo.add(avisoIngreso, c);
        c.gridy = 5;
        c.weighty = 1;
        cuerpo.add(javax.swing.Box.createGlue(), c);

        Tarjeta tarjeta = new Tarjeta("Registrar ingreso");
        tarjeta.setToolTipText("RF-02");
        return tarjeta.contenido(cuerpo);
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
        javax.swing.JLabel l = new javax.swing.JLabel(titulo);
        l.setFont(Tema.ETIQUETA);
        l.setForeground(Tema.TEXTO_SUAVE);
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
        if (Tema.esUrgente(nivel)) {
            chipPrioridad.configurar(nivel.getEtiqueta(), Tema.color(nivel), true);
        } else {
            chipPrioridad.texto(nivel.getEtiqueta(), null);
        }
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
                throw new Formularios.EntradaInvalida("El DNI debe tener exactamente " + Damnificado.LONGITUD_DNI
                        + " dígitos, sin letras ni espacios.", txtDni);
            }
            String nombre = Formularios.textoObligatorio(txtNombre, "El nombre");
            String dniTutor = null;
            String nombreTutor = null;
            String parentesco = null;
            if (esMenor() && chkTutor.isSelected()) {
                dniTutor = txtDniTutor.getText().trim();
                if (!Damnificado.esDniValido(dniTutor)) {
                    throw new Formularios.EntradaInvalida("El DNI del tutor debe tener 8 dígitos.", txtDniTutor);
                }
                if (dniTutor.equals(dni)) {
                    throw new Formularios.EntradaInvalida("El DNI del tutor no puede ser el del propio menor.", txtDniTutor);
                }
                nombreTutor = Formularios.textoObligatorio(txtNombreTutor, "El nombre del tutor");
                parentesco = Formularios.textoObligatorio(txtParentesco, "El parentesco");
            }
            Damnificado nuevo = crearPersona(dni, nombre, dniTutor, nombreTutor, parentesco);
            ResultadoIngreso r = controller.registrarIngreso(albergue.getId(), nuevo);
            switch (r) {
                case INGRESADO -> confirmarIngreso(nuevo, albergue);
                case IDENTIDAD_DUPLICADA -> avisoIngreso.error(
                        "Ese DNI o ese nombre ya está registrado en la red. Búscalo en «Ubicar en la red».", txtDni);
                case ALBERGUE_NO_ENCONTRADO -> avisoIngreso.error("El albergue elegido ya no existe.", cmbAlbergueIngreso);
                case SIN_CUPO -> ofrecerOtroAlbergue(nuevo, albergue);
            }
        } catch (Formularios.EntradaInvalida ex) {
            avisoIngreso.error(ex);
        } catch (IllegalArgumentException ex) {
            avisoIngreso.error(ex.getMessage(), null);
        }
    }

    private void confirmarIngreso(Damnificado d, Albergue albergue) {
        limpiarFormulario();
        avisoIngreso.exito("Ingreso registrado: " + d.getId() + " · " + d.getNombreCompleto() + " en "
                + albergue.getNombre() + ". Prioridad " + d.getNivelPrioridad().getEtiqueta() + ".");
        txtDni.requestFocusInWindow(); // listo para el siguiente ingreso
    }

    /** RF-08: el albergue está lleno; se ofrecen los que tienen cupo. Pregunta antes de mover a nadie. */
    private void ofrecerOtroAlbergue(Damnificado d, Albergue lleno) {
        List<Albergue> alternativas = controller.alberguesConCupo(lleno.getId());
        if (alternativas.isEmpty()) {
            avisoIngreso.error(lleno.getNombre() + " está lleno y no hay otros albergues con cupo en la red.", cmbAlbergueIngreso);
            return;
        }
        String[] opciones = alternativas.stream()
                .map(a -> a.getId() + " · " + a.getNombre() + " (" + a.getCuposDisponibles() + " cupos libres)")
                .toArray(String[]::new);
        Object eleccion = JOptionPane.showInputDialog(this,
                lleno.getNombre() + " no tiene cupo.\nElige un albergue con cupo para esta persona:",
                "Albergue lleno", JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);
        if (eleccion == null) {
            avisoIngreso.error("Ingreso cancelado: " + lleno.getNombre() + " no tiene cupo.", cmbAlbergueIngreso);
            return;
        }
        Albergue destino = alternativas.get(java.util.Arrays.asList(opciones).indexOf(eleccion));
        ResultadoIngreso r = controller.registrarIngreso(destino.getId(), d);
        if (r == ResultadoIngreso.INGRESADO) {
            confirmarIngreso(d, destino);
        } else {
            avisoIngreso.error("No se pudo alojar en " + destino.getNombre() + ". Intenta con otro albergue.", cmbAlbergueIngreso);
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
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 14, 0);

        // Derivar
        cmbOrigen.addActionListener(e -> {
            if (!refrescando) {
                recordarAlbergue(cmbOrigen);
                recargarPersonas(cmbOrigen, cmbPersonaDerivar);
                recargarDestinos();
            }
        });
        btnDerivar = new BotonModerno("Trasladar");
        btnDerivar.setIcon(Iconos.de(Iconos.Figura.CAMION, 16, Tema.TINTA_ACCION));
        btnDerivar.addActionListener(e -> derivar());
        Formularios.Rejilla derivar = new Formularios.Rejilla()
                .fila("Albergue de origen", cmbOrigen)
                .fila("Persona", cmbPersonaDerivar)
                .fila("Albergue de destino", cmbDestino)
                .completa(alinearDerecha(btnDerivar))
                .completa(avisoDerivar);
        Tarjeta tDerivar = new Tarjeta("Derivar a otro albergue");
        tDerivar.setToolTipText("RF-08");
        c.gridy = 0;
        columna.add(tDerivar.contenido(derivar), c);

        // Salida
        cmbAlbergueSalida.addActionListener(e -> {
            if (!refrescando) {
                recordarAlbergue(cmbAlbergueSalida);
                recargarPersonas(cmbAlbergueSalida, cmbPersonaSalida);
            }
        });
        BotonModerno btnSalida = new BotonModerno("Registrar salida", BotonModerno.Estilo.PELIGRO);
        btnSalida.setIcon(Iconos.de(Iconos.Figura.SALIDA, 16, Color.WHITE));
        btnSalida.addActionListener(e -> registrarSalida());
        Formularios.Rejilla salida = new Formularios.Rejilla()
                .fila("Albergue", cmbAlbergueSalida)
                .fila("Persona", cmbPersonaSalida)
                .completa(alinearDerecha(btnSalida))
                .completa(avisoSalida);
        Tarjeta tSalida = new Tarjeta("Salida voluntaria");
        tSalida.setToolTipText("RF-08");
        c.gridy = 1;
        columna.add(tSalida.contenido(salida), c);

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
                .completa(avisoUbicar)
                .completa(Formularios.nota("Muestra dónde está alojada la persona y permite registrar la atención "
                        + "post-médica de personas vulnerables."));
        c.gridy = 2;
        columna.add(new Tarjeta("Ubicar en la red").contenido(ubicar), c);

        c.gridy = 3;
        c.weighty = 1;
        c.insets = new Insets(0, 0, 0, 0);
        columna.add(javax.swing.Box.createGlue(), c);
        return columna;
    }

    private static JPanel alinearDerecha(JComponent c) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        p.setOpaque(false);
        p.add(c);
        return p;
    }

    /** Cuando la persona elige un albergue, ese pasa a ser el albergue en foco de toda la app. */
    private void recordarAlbergue(JComboBox<Albergue> combo) {
        if (!refrescando && combo.getSelectedItem() instanceof Albergue a) {
            contexto.setIdAlbergue(a.getId());
        }
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
                throw new Formularios.EntradaInvalida("Este albergue no tiene personas para trasladar.", cmbOrigen);
            }
            Albergue destino = (Albergue) cmbDestino.getSelectedItem();
            if (destino == null) {
                throw new Formularios.EntradaInvalida("No hay otro albergue con cupo libre en la red.", cmbDestino);
            }
            String codigoAnterior = persona.getId();
            ResultadoDerivacion r = controller.derivar(origen.getId(), destino.getId(), codigoAnterior);
            switch (r) {
                case DERIVADO -> avisoDerivar.exito(persona.getNombreCompleto() + " ahora está en " + destino.getNombre()
                        + ". Su código cambió de " + codigoAnterior + " a " + persona.getId() + ".");
                case MISMO_ALBERGUE -> avisoDerivar.error("El origen y el destino no pueden ser el mismo albergue.", cmbDestino);
                case DESTINO_SIN_CUPO -> avisoDerivar.error(destino.getNombre() + " ya no tiene cupo. Elige otro destino.", cmbDestino);
                default -> avisoDerivar.error("No se pudo trasladar. Vuelve a elegir a la persona.", cmbPersonaDerivar);
            }
        } catch (Formularios.EntradaInvalida ex) {
            avisoDerivar.error(ex);
        }
    }

    private void registrarSalida() {
        try {
            Albergue albergue = Formularios.albergue(cmbAlbergueSalida);
            Damnificado persona = (Damnificado) cmbPersonaSalida.getSelectedItem();
            if (persona == null) {
                throw new Formularios.EntradaInvalida("Este albergue no tiene personas alojadas.", cmbAlbergueSalida);
            }
            // Acción que no se puede deshacer: se confirma nombrando a la persona y el efecto
            if (!Formularios.confirmar(this, "Confirmar salida", "¿Registrar la salida de " + persona.getNombreCompleto()
                    + " (" + persona.getId() + ")?\nSe liberará su cupo en " + albergue.getNombre()
                    + ". Esta acción no se puede deshacer.")) {
                return;
            }
            if (controller.registrarSalida(albergue.getId(), persona.getId())) {
                avisoSalida.exito("Salida registrada: " + persona.getNombreCompleto() + ". Cupo liberado en "
                        + albergue.getNombre() + ".");
            } else {
                avisoSalida.error("Esa persona ya no está alojada en este albergue.", cmbPersonaSalida);
            }
        } catch (Formularios.EntradaInvalida ex) {
            avisoSalida.error(ex);
        }
    }

    private void ubicarPorDni() {
        String dni = txtDniBuscar.getText().trim();
        if (!Damnificado.esDniValido(dni)) {
            avisoUbicar.error("Escribe un DNI de 8 dígitos.", txtDniBuscar);
            return;
        }
        Optional<Albergue> encontrado = controller.ubicarPorDni(dni);
        if (encontrado.isEmpty()) {
            avisoUbicar.error("No hay nadie con DNI " + dni + " en la red.", txtDniBuscar);
            return;
        }
        Albergue albergue = encontrado.get();
        Damnificado d = albergue.buscarDamnificado(dni).orElseThrow();
        String info = d.getNombreCompleto() + " (" + d.getId() + ") está en " + albergue.getNombre()
                + ". Prioridad " + d.getNivelPrioridad().getEtiqueta() + ".";
        if (d instanceof DamnificadoVulnerable v) {
            info += " Atendido/a: " + (v.isAtendido() ? "sí" : "no") + " · Estable: " + (v.isEstable() ? "sí" : "no") + ".";
            avisoUbicar.exito(info);
            if (Formularios.confirmar(this, "Verificación post-atención", info + "\n\n¿Registrar su verificación post-atención?")) {
                boolean atendido = Formularios.confirmar(this, "Verificación", "¿Ya fue atendido/a por el personal?");
                boolean estable = Formularios.confirmar(this, "Estado post-atención", "¿Se encuentra estable tras la atención?");
                controller.registrarVerificacion(v, atendido, estable);
                avisoUbicar.exito(d.getNombreCompleto() + ": " + (atendido ? "atendido/a" : "aún no atendido/a")
                        + " · " + (estable ? "estable" : "en recuperación") + ".");
            }
        } else {
            avisoUbicar.exito(info);
        }
    }

    // =========================================================
    // CONTEXTO COMPARTIDO Y REFRESCO
    // =========================================================

    /** Al mostrarse: todos los combos de albergue apuntan al albergue en foco. */
    public void alMostrar() {
        String id = contexto.getIdAlbergue();
        if (id == null) {
            return;
        }
        refrescando = true;
        try {
            for (JComboBox<Albergue> combo : List.of(cmbAlbergueIngreso, cmbOrigen, cmbAlbergueSalida)) {
                for (int i = 0; i < combo.getItemCount(); i++) {
                    if (combo.getItemAt(i).getId().equalsIgnoreCase(id)) {
                        combo.setSelectedIndex(i);
                    }
                }
            }
            recargarPersonas(cmbOrigen, cmbPersonaDerivar);
            recargarPersonas(cmbAlbergueSalida, cmbPersonaSalida);
            recargarDestinos();
        } finally {
            refrescando = false;
        }
    }

    /** Llegada desde una alerta de ocupación: el foco va directo al traslado. */
    public void prepararDerivacion() {
        alMostrar();
        avisoDerivar.limpiar();
        cmbPersonaDerivar.requestFocusInWindow();
    }

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
