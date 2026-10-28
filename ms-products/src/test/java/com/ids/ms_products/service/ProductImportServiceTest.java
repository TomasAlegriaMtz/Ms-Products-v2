
package com.ids.ms_products.service;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import com.ids.ms_products.dto.ImportResultDTO;
import com.ids.ms_products.dto.ProductRegisterRequest;
import com.ids.ms_products.exceptions.InvalidCsvException;
import com.ids.ms_products.exceptions.ProductAlreadyExistsException;
import com.ids.ms_products.exceptions.ProductException;
import com.ids.ms_products.service.impl.ProductImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.*;


class ProductImportServiceTest {

    private ProductImportService importService;
    private ProductService productService;

    @BeforeEach
    void setUp() {

        Validator validator = Validation
                .buildDefaultValidatorFactory()
                .getValidator();

        productService = mock(ProductService.class);

        importService = new ProductImportService(
                validator,
                productService
        );
    }

    @Test
    void debeImportarProductoValido() throws Exception {

        String csv = """
                name,description,price,stock,categoryId
                Producto Test,Producto de prueba,150,150,1
                """;

        MockMultipartFile archivo = new MockMultipartFile(
                "file",
                "productos.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        ImportResultDTO resultado =
                importService.procesarCSV(archivo);

        assertEquals(1, resultado.getProcesados());
        assertEquals(1, resultado.getAceptados());
        assertEquals(0, resultado.getRechazados());

        verify(productService, times(1))
                .saveProduct(any());
    }


    @Test
    void debeRechazarProductoConPrecioNoNumerico() throws Exception {

        String csv = """
            name,description,price,stock,categoryId
            Producto Test,Producto de prueba,abc,150,1
            """;

        MockMultipartFile archivo = new MockMultipartFile(
                "file",
                "productos.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        ImportResultDTO resultado =
                importService.procesarCSV(archivo);

        assertEquals(1, resultado.getProcesados());
        assertEquals(0, resultado.getAceptados());
        assertEquals(1, resultado.getRechazados());

        assertEquals(1, resultado.getErrores().size());
        assertEquals(2, resultado.getErrores().get(0).getFila());

        verify(productService, never()).saveProduct(any());
    }


    @Test
    void debeRechazarCsvMalFormadoSinGuardarProductos() throws Exception {

        String csv = """
            name,description,price,stock,categoryId
            Producto Test A,Producto valido,150,150,1
            Producto Test B,"Descripcion sin cerrar,250,200,1
            """;

        MockMultipartFile archivo = new MockMultipartFile(
                "file",
                "productos-malformados.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        InvalidCsvException excepcion = assertThrows(
                InvalidCsvException.class,
                () -> importService.procesarCSV(archivo)
        );

        assertEquals(
                "El archivo CSV tiene un formato incorrecto",
                excepcion.getMessage()
        );

        verify(productService, never()).saveProduct(any());
    }




    @Test
    void debeRechazarProductoConStockInsuficiente() throws Exception {

        // 1. Crear un CSV con stock inferior al mínimo
        String csv = """
            name,description,price,stock,categoryId
            Producto Stock Test,Producto de prueba,150,50,1
            """;

        // 2. Simular el archivo que recibiríamos desde Postman
        MockMultipartFile archivo = new MockMultipartFile(
                "file",
                "productos.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        // 3. Simular el comportamiento del servicio de productos
        when(productService.saveProduct(
                ArgumentMatchers.any(ProductRegisterRequest.class)
        )).thenThrow(
                new ProductException(
                        "El stock mínimo requerido es de 100 unidades"
                )
        );

        // 4. Ejecutar nuestro servicio de importación
        ImportResultDTO resultado =
                importService.procesarCSV(archivo);

        // 5. Comprobar los contadores
        assertEquals(1, resultado.getProcesados());
        assertEquals(0, resultado.getAceptados());
        assertEquals(1, resultado.getRechazados());

        // 6. Verificar el motivo del rechazo
        assertEquals(1, resultado.getErrores().size());

        assertEquals(
                "El stock mínimo requerido es de 100 unidades",
                resultado.getErrores().get(0).getMensaje()
        );

        assertEquals(
                2,
                resultado.getErrores().get(0).getFila()
        );

        // 7. Verificar que se intentó registrar el producto
        verify(productService, times(1))
                .saveProduct(any(ProductRegisterRequest.class));
    }


    @Test
    void debeRechazarProductoDuplicado() throws Exception {

        // 1. Simulamos el contenido de un CSV
        String csv = """
            name,description,price,stock,categoryId
            Laptop Lenovo,Laptop de prueba,15000,150,1
            """;

        MockMultipartFile archivo = new MockMultipartFile(
                "file",
                "productos-duplicados.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        // 2. Simulamos que el producto ya existe
        when(productService.saveProduct(
                any(ProductRegisterRequest.class)
        )).thenThrow(
                new ProductAlreadyExistsException(
                        "Ya existe un producto registrado con el nombre Laptop Lenovo"
                )
        );

        // 3. Ejecutamos el procesamiento
        ImportResultDTO resultado =
                importService.procesarCSV(archivo);

        // 4. Comprobamos los contadores
        assertEquals(1, resultado.getProcesados());
        assertEquals(0, resultado.getAceptados());
        assertEquals(1, resultado.getRechazados());

        // 5. Verificamos el error registrado
        assertEquals(1, resultado.getErrores().size());

        assertEquals(
                2,
                resultado.getErrores().get(0).getFila()
        );

        assertEquals(
                "Ya existe un producto registrado con el nombre Laptop Lenovo",
                resultado.getErrores().get(0).getMensaje()
        );

        // 6. Confirmamos que se intentó guardar una vez
        verify(productService, times(1))
                .saveProduct(any(ProductRegisterRequest.class));
    }


    @Test
    void debeRealizarImportacionParcial() throws Exception {

        // 1. CSV con tres productos
        String csv = """
            name,description,price,stock,categoryId
            Producto Test A,Producto valido,150,150,1
            Producto Test B,Stock insuficiente,250,50,1
            Producto Test C,Producto valido,350,120,1
            """;

        MockMultipartFile archivo = new MockMultipartFile(
                "file",
                "productos-parcial.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        // 2. Simular error solamente para el producto B
        when(productService.saveProduct(
                argThat(producto ->
                        "Producto Test B".equals(producto.getName())
                )
        )).thenThrow(
                new ProductException(
                        "El stock mínimo requerido es de 100 unidades"
                )
        );

        // 3. Ejecutar importación
        ImportResultDTO resultado =
                importService.procesarCSV(archivo);

        // 4. Validar los contadores
        assertEquals(3, resultado.getProcesados());
        assertEquals(2, resultado.getAceptados());
        assertEquals(1, resultado.getRechazados());

        // 5. Comprobar el error de la fila 3
        assertEquals(1, resultado.getErrores().size());

        assertEquals(
                3,
                resultado.getErrores().get(0).getFila()
        );

        assertEquals(
                "El stock mínimo requerido es de 100 unidades",
                resultado.getErrores().get(0).getMensaje()
        );

        // 6. Verificar que se intentaron procesar los tres
        verify(productService, times(3))
                .saveProduct(any(ProductRegisterRequest.class));

        // 7. Comprobar que el producto C se procesó
        // después del error del producto B
        verify(productService).saveProduct(
                argThat(producto ->
                        "Producto Test C".equals(producto.getName())
                )
        );
    }


    @Test
    void debeRechazarCsvConEncabezadosIncorrectos() throws Exception {

        // 1. Crear un CSV con encabezados incorrectos
        String csv = """
            nombre,descripcion,precio,existencias,categoria
            Laptop Lenovo,Laptop de prueba,15000,150,1
            """;

        // 2. Simular el archivo recibido desde Postman
        MockMultipartFile archivo = new MockMultipartFile(
                "file",
                "productos-incorrectos.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        // 3. Comprobar que la validación rechaza el archivo
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> importService.validarEncabezados(archivo)
        );

        // 4. Comprobar que el mensaje explica el problema
        assertTrue(
                excepcion.getMessage().contains(
                        "Los encabezados del CSV no son válidos"
                )
        );

        // 5. Verificar que no se intentó guardar ningún producto
        verify(productService, never())
                .saveProduct(any(ProductRegisterRequest.class));
    }

    @Test
    void debeValidarArchivoCorrectamente() {
        assertThrows(InvalidCsvException.class, () -> importService.validarArchivo(null));

        MockMultipartFile vacio = new MockMultipartFile("file", "test.csv", "text/csv", new byte[0]);
        assertThrows(InvalidCsvException.class, () -> importService.validarArchivo(vacio));

        MockMultipartFile sinNombre = new MockMultipartFile("file", null, "text/csv", "datos".getBytes(StandardCharsets.UTF_8));
        assertThrows(InvalidCsvException.class, () -> importService.validarArchivo(sinNombre));

        MockMultipartFile txt = new MockMultipartFile("file", "test.txt", "text/plain", "datos".getBytes(StandardCharsets.UTF_8));
        assertThrows(InvalidCsvException.class, () -> importService.validarArchivo(txt));

        byte[] payloadGrande = new byte[6 * 1024 * 1024];
        MockMultipartFile grande = new MockMultipartFile("file", "grande.csv", "text/csv", payloadGrande);
        assertThrows(InvalidCsvException.class, () -> importService.validarArchivo(grande));

        MockMultipartFile valido = new MockMultipartFile("file", "valido.csv", "text/csv", "name,description,price,stock,categoryId\n".getBytes(StandardCharsets.UTF_8));
        assertDoesNotThrow(() -> importService.validarArchivo(valido));
    }

    @Test
    void debeRechazarEncabezadosEnArchivoVacio() {
        MockMultipartFile vacio = new MockMultipartFile("file", "vacio.csv", "text/csv", new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> importService.validarEncabezados(vacio));
    }

    @Test
    void debeLeerRegistrosCSVCorrectamente() throws Exception {
        String csv = """
                name,description,price,stock,categoryId
                Monitor Dell,Monitor 24 pulgadas,2500,10,2
                """;
        MockMultipartFile archivo = new MockMultipartFile("file", "productos.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        var lista = importService.leerRegistrosCSV(archivo);
        assertNotNull(lista);
        assertEquals(1, lista.size());
        assertEquals("Monitor Dell", lista.get(0).getName());
        assertEquals(10, lista.get(0).getStock());
    }

    @Test
    void debeLanzarExcepcionSiCsvNoTieneRegistros() throws Exception {
        String csv = """
                name,description,price,stock,categoryId
                """;
        MockMultipartFile archivo = new MockMultipartFile("file", "vacio.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> importService.procesarCSV(archivo));
    }

    @Test
    void debeRechazarFilaConColumnasInconsistentes() throws Exception {
        String csv = """
                name,description,price,stock,categoryId
                Teclado Mecanico,Teclado RGB,500
                """;
        MockMultipartFile archivo = new MockMultipartFile("file", "inconsistente.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        ImportResultDTO resultado = importService.procesarCSV(archivo);
        assertEquals(1, resultado.getProcesados());
        assertEquals(0, resultado.getAceptados());
        assertEquals(1, resultado.getRechazados());
        assertEquals(1, resultado.getErrores().size());
        assertTrue(resultado.getErrores().get(0).getMensaje().contains("columnas incompletas"));
    }

    @Test
    void debeRechazarProductoCuandoFallaValidacionJakarta() throws Exception {
        String csv = """
                name,description,price,stock,categoryId
                ,Descripcion valida,150,150,1
                """;
        MockMultipartFile archivo = new MockMultipartFile("file", "invalido.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        ImportResultDTO resultado = importService.procesarCSV(archivo);
        assertEquals(1, resultado.getProcesados());
        assertEquals(0, resultado.getAceptados());
        assertEquals(1, resultado.getRechazados());
        assertEquals(1, resultado.getErrores().size());
    }


}
