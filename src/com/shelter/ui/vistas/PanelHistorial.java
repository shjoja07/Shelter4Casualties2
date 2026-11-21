package com.shelter.ui.vistas;

import com.shelter.controller.ShelterController;
import com.shelter.model.RegistroAsignacion;
import com.shelter.persistence.PersistenciaJson;
import com.shelter.ui.componentes.BotonModerno;
import com.shelter.ui.componentes.Formularios;
import com.shelter.ui.componentes.Iconos;
import com.shelter.ui.componentes.Renderizadores;
import com.shelter.ui.componentes.Tarjeta;
import com.shelter.ui.tema.Tema;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Historial de entregas (constancias con marca de tiempo, RNF-06) y persistencia en JSON.
 */
public class PanelHistorial extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final transient ShelterController controller;
    private final ModeloHistorial modelo = new ModeloHistorial();
    private final JTextField txtBuscar = Formularios.campo("");
    private final JLabel lblTotal = Formularios.etiqueta(" ");
    private final JLabel lblArchivo = Formularios.etiqueta("Archivo: " + ShelterController.RUTA_DATOS.toAbsolutePath());

    public PanelHistorial(ShelterController controller) {
        super(new BorderLayout(0, 14));
        this.controller = controller;
        setOpaque(false);

        JTable tabla = Renderizadores.tabla(modelo,
                () -> controller.historial().isEmpty()
                        ? "Todavía no hay entregas. Ejecuta un reparto en la sección Recursos."
                        : "Ninguna entrega coincide con la búsqueda.");
        Tema.estilizar(tabla);
        tabla.setAutoCreateRowSorter(true);
        tabla.setDefaultRenderer(Object.class, new Renderizadores.Texto());
        tabla.getColumnModel().getColumn(2).setCellRenderer(new Renderizadores.Texto(true));

        txtBuscar.setColumns(22);
        txtBuscar.setToolTipText("Filtra por albergue, código, DNI, nombre o recurso");
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refrescar(); }
            @Override public void removeUpdate(DocumentEvent e) { refrescar(); }
            @Override public void changedUpdate(DocumentEvent e) { refrescar(); }
        });
        JPanel buscar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buscar.setOpaque(false);
        buscar.add(lblTotal);
        buscar.add(new JLabel(Iconos.de(Iconos.Figura.LUPA, 16, Tema.TEXTO_SUAVE)));
        buscar.add(txtBuscar);

        Tarjeta tarjetaTabla = new Tarjeta("Historial de entregas de recursos");
        tarjetaTabla.accionesTitulo(buscar);
        tarjetaTabla.contenido(Tema.scroll(tabla));

        BotonModerno btnGuardar = new BotonModerno("Guardar", BotonModerno.Estilo.PRIMARIO);
        btnGuardar.setIcon(Iconos.de(Iconos.Figura.GUARDAR, 16, Tema.TINTA_ACCION));
        btnGuardar.addActionListener(e -> guardar(ShelterController.RUTA_DATOS));
        BotonModerno btnExportar = new BotonModerno("Exportar como…", BotonModerno.Estilo.SECUNDARIO);
        btnExportar.addActionListener(e -> {
            JFileChooser fc = selectorJson();
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                Path ruta = fc.getSelectedFile().toPath();
                if (!ruta.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json")) {
                    ruta = ruta.resolveSibling(ruta.getFileName() + ".json");
                }
                guardar(ruta);
            }
        });
        BotonModerno btnCargar = new BotonModerno("Cargar archivo…", BotonModerno.Estilo.SECUNDARIO);
        btnCargar.setIcon(Iconos.de(Iconos.Figura.ABRIR, 16, Tema.ACENTO));
        btnCargar.addActionListener(e -> {
            JFileChooser fc = selectorJson();
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                cargar(fc.getSelectedFile().toPath());
            }
        });

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        botones.setOpaque(false);
        botones.add(btnGuardar);
        botones.add(btnExportar);
        botones.add(btnCargar);
        botones.add(lblArchivo);

        add(tarjetaTabla, BorderLayout.CENTER);
        add(new Tarjeta("Persistencia JSON (al cerrar se guarda automáticamente)").contenido(botones), BorderLayout.SOUTH);
    }

    private static JFileChooser selectorJson() {
        JFileChooser fc = new JFileChooser(Path.of("").toAbsolutePath().toFile());
        fc.setFileFilter(new FileNameExtensionFilter("Datos JSON (*.json)", "json"));
        return fc;
    }

    private void guardar(Path ruta) {
        try {
            controller.guardar(ruta);
            lblArchivo.setText("Guardado en: " + ruta.toAbsolutePath());
            Formularios.info(this, "Datos guardados", "Estado guardado en:\n" + ruta.toAbsolutePath());
        } catch (IOException ex) {
            Formularios.error(this, "No se pudo guardar: " + ex.getMessage());
        }
    }

    private void cargar(Path ruta) {
        try {
            PersistenciaJson.Datos datos = controller.cargar(ruta);
            lblArchivo.setText("Cargado desde: " + ruta.toAbsolutePath());
            Formularios.info(this, "Datos cargados", datos.albergues().size() + " albergue(s) y "
                    + datos.historial().size() + " entrega(s) en el historial.");
        } catch (IOException | IllegalArgumentException ex) {
            Formularios.error(this, "Archivo inválido; los datos actuales no se modificaron.\n" + ex.getMessage());
        }
    }

    public void refrescar() {
        String filtro = txtBuscar.getText().trim().toLowerCase(Locale.ROOT);
        List<RegistroAsignacion> todos = controller.historial();
        List<RegistroAsignacion> visibles = new ArrayList<>();
        for (RegistroAsignacion r : todos) {
            String texto = String.join(" ", r.getIdAlbergue(), r.getIdDamnificado(), r.getDni(),
                    r.getNombreDamnificado(), r.getRecurso()).toLowerCase(Locale.ROOT);
            if (filtro.isEmpty() || texto.contains(filtro)) {
                visibles.add(r);
            }
        }
        Collections.reverse(visibles); // lo más reciente primero
        modelo.setDatos(visibles);
        lblTotal.setText(visibles.size() + " de " + todos.size() + " entregas");
    }

    private static final class ModeloHistorial extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private static final String[] COLUMNAS = {"Fecha y hora", "Albergue", "Código", "DNI", "Damnificado",
                "Recurso", "Cantidad"};
        private transient List<RegistroAsignacion> datos = List.of();

        void setDatos(List<RegistroAsignacion> nuevos) {
            datos = List.copyOf(nuevos);
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return datos.size(); }
        @Override public int getColumnCount() { return COLUMNAS.length; }
        @Override public String getColumnName(int c) { return COLUMNAS[c]; }

        @Override
        public Object getValueAt(int fila, int col) {
            RegistroAsignacion r = datos.get(fila);
            return switch (col) {
                case 0 -> r.getFechaHora().format(FORMATO);
                case 1 -> r.getIdAlbergue();
                case 2 -> r.getIdDamnificado();
                case 3 -> r.getDni();
                case 4 -> r.getNombreDamnificado();
                case 5 -> r.getRecurso();
                default -> com.shelter.ui.componentes.Formatos.cantidad(r.getCantidad(), r.getUnidad());
            };
        }
    }
}
