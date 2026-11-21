package com.shelter.ui.componentes;

import com.shelter.model.Albergue;
import com.shelter.ui.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Utilidades de la vista: construcción de formularios alineados, lectura VALIDADA de campos y
 * mensajes al usuario. Centralizar la validación evita repetir los mismos try/catch en cada panel.
 */
public final class Formularios {

    private Formularios() {
    }

    /** Error de validación de un campo: se muestra al usuario y no se modifica ningún dato. */
    public static class EntradaInvalida extends Exception {
        private static final long serialVersionUID = 1L;

        public EntradaInvalida(String mensaje) {
            super(mensaje);
        }
    }

    // =========================================================
    // CONSTRUCCIÓN
    // =========================================================

    /** Formulario de dos columnas (etiqueta | campo) que crece hacia abajo. */
    public static class Rejilla extends JPanel {
        private static final long serialVersionUID = 1L;
        private int fila;

        public Rejilla() {
            super(new GridBagLayout());
            setOpaque(false);
        }

        public Rejilla fila(String etiqueta, JComponent campo) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridy = fila++;
            c.insets = new Insets(5, 0, 5, 12);
            c.anchor = GridBagConstraints.WEST;
            c.gridx = 0;
            add(etiqueta(etiqueta), c);
            c.gridx = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(5, 0, 5, 0);
            add(campo, c);
            return this;
        }

        /** Componente que ocupa las dos columnas. */
        public Rejilla completa(JComponent componente) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridy = fila++;
            c.gridx = 0;
            c.gridwidth = 2;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(6, 0, 6, 0);
            add(componente, c);
            return this;
        }

        /** Relleno final para que el formulario quede arriba. */
        public Rejilla empujar() {
            GridBagConstraints c = new GridBagConstraints();
            c.gridy = fila++;
            c.weighty = 1;
            c.gridwidth = 2;
            c.fill = GridBagConstraints.BOTH;
            JPanel vacio = new JPanel();
            vacio.setOpaque(false);
            add(vacio, c);
            return this;
        }
    }

    public static JLabel etiqueta(String texto) {
        JLabel l = new JLabel(texto);
        l.setForeground(Tema.TEXTO_SUAVE);
        l.setFont(Tema.TEXTO_NORMAL);
        return l;
    }

    public static JLabel nota(String texto) {
        JLabel l = new JLabel("<html>" + texto + "</html>");
        l.setForeground(Tema.TEXTO_TENUE);
        l.setFont(Tema.TEXTO_PEQUENO);
        return l;
    }

    public static JTextField campo(String inicial) {
        JTextField t = new JTextField(inicial);
        t.setForeground(Tema.TEXTO);
        t.setCaretColor(Tema.ACENTO);
        t.setBackground(Tema.CAMPO);
        return t;
    }

    public static JCheckBox casilla(String texto) {
        JCheckBox c = new JCheckBox(texto);
        c.setOpaque(false);
        c.setForeground(Tema.TEXTO);
        return c;
    }

    // =========================================================
    // COMBOS DE OBJETOS (sin parsear texto)
    // =========================================================

    /**
     * Recarga un combo conservando la selección por clave. Trabajar con objetos (y no con textos
     * "A1 - Nombre" que luego se cortan con split) evita errores si un nombre contiene " - ".
     */
    public static <T> void recargar(JComboBox<T> combo, List<T> elementos, Function<T, String> clave) {
        int indice = combo.getSelectedIndex();
        String claveAnterior = indice < 0 ? null : clave.apply(combo.getItemAt(indice));
        DefaultComboBoxModel<T> modelo = new DefaultComboBoxModel<>();
        T aSeleccionar = null;
        for (T e : elementos) {
            modelo.addElement(e);
            if (claveAnterior != null && Objects.equals(clave.apply(e), claveAnterior)) {
                aSeleccionar = e;
            }
        }
        combo.setModel(modelo);
        if (aSeleccionar != null) {
            combo.setSelectedItem(aSeleccionar);
        } else if (modelo.getSize() > 0) {
            combo.setSelectedIndex(0);
        }
    }

    public static Albergue albergue(JComboBox<Albergue> combo) throws EntradaInvalida {
        Albergue a = (Albergue) combo.getSelectedItem();
        if (a == null) {
            throw new EntradaInvalida("Seleccione un albergue.");
        }
        return a;
    }

    // =========================================================
    // LECTURA VALIDADA
    // =========================================================

    /** Decimal que acepta coma o punto. */
    public static double decimal(JTextField campo, String nombre, boolean permitirCero) throws EntradaInvalida {
        String texto = campo.getText().trim().replace(',', '.');
        try {
            double v = Double.parseDouble(texto);
            if (!Double.isFinite(v) || v < 0 || (!permitirCero && v == 0)) {
                throw new NumberFormatException();
            }
            return v;
        } catch (NumberFormatException e) {
            campo.requestFocusInWindow();
            throw new EntradaInvalida(nombre + " debe ser un número " + (permitirCero ? "mayor o igual a 0." : "mayor que 0."));
        }
    }

    /** Fecha AAAA-MM-DD opcional (vacío = no vence). */
    public static LocalDate fechaOpcional(JTextField campo) throws EntradaInvalida {
        String texto = campo.getText().trim();
        if (texto.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(texto);
        } catch (DateTimeParseException e) {
            campo.requestFocusInWindow();
            throw new EntradaInvalida("Fecha inválida. Use AAAA-MM-DD (ej. 2027-05-20) o déjela vacía.");
        }
    }

    public static String textoObligatorio(JTextField campo, String nombre) throws EntradaInvalida {
        String texto = campo.getText().trim();
        if (texto.isEmpty()) {
            campo.requestFocusInWindow();
            throw new EntradaInvalida(nombre + " es obligatorio.");
        }
        return texto;
    }

    // =========================================================
    // MENSAJES
    // =========================================================

    public static void error(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "No se pudo completar", JOptionPane.ERROR_MESSAGE);
    }

    public static void aviso(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "Atención", JOptionPane.WARNING_MESSAGE);
    }

    public static void info(Component padre, String titulo, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, titulo, JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirmar(Component padre, String titulo, Object mensaje) {
        return JOptionPane.showConfirmDialog(padre, mensaje, titulo, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    /** Envoltorio con margen para usar un componente dentro de un JOptionPane. */
    public static JPanel conMargen(JComponent c) {
        JPanel p = new JPanel(new java.awt.BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));
        p.add(c);
        return p;
    }
}
