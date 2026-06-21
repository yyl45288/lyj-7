package com.warehouse.wavepicking.service;

import com.warehouse.wavepicking.dto.response.SkuResponse;
import com.warehouse.wavepicking.entity.Sku;
import com.warehouse.wavepicking.exception.BusinessException;
import com.warehouse.wavepicking.repository.SkuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkuServiceTest {

    @Mock
    private SkuRepository skuRepository;

    @InjectMocks
    private SkuService skuService;

    private Sku testSku;

    @BeforeEach
    void setUp() {
        testSku = new Sku();
        testSku.setId(1L);
        testSku.setSkuCode("SKU001");
        testSku.setSkuName("测试商品");
        testSku.setCategory("电子产品");
        testSku.setUnit("件");
        testSku.setWeight(1.0);
        testSku.setLocation("A01");
        testSku.setCreatedAt(LocalDateTime.now());
        testSku.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("获取所有SKU列表")
    void testGetAllSkus() {
        Sku sku2 = new Sku();
        sku2.setId(2L);
        sku2.setSkuCode("SKU002");
        sku2.setSkuName("商品2");

        when(skuRepository.findAll()).thenReturn(Arrays.asList(testSku, sku2));

        List<SkuResponse> result = skuService.getAllSkus();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("SKU001", result.get(0).getSkuCode());
        verify(skuRepository).findAll();
    }

    @Test
    @DisplayName("根据ID获取SKU - 成功")
    void testGetSkuById_Success() {
        when(skuRepository.findById(1L)).thenReturn(Optional.of(testSku));

        SkuResponse result = skuService.getSkuById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("SKU001", result.getSkuCode());
        assertEquals("测试商品", result.getSkuName());
    }

    @Test
    @DisplayName("根据ID获取SKU - 不存在")
    void testGetSkuById_NotFound() {
        when(skuRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> skuService.getSkuById(999L));

        assertEquals("SKU_NOT_FOUND", exception.getCode());
    }

    @Test
    @DisplayName("创建SKU - 成功")
    void testCreateSku_Success() {
        Sku newSku = new Sku();
        newSku.setSkuCode("SKU003");
        newSku.setSkuName("新商品");

        when(skuRepository.existsBySkuCode("SKU003")).thenReturn(false);
        when(skuRepository.save(any(Sku.class))).thenAnswer(invocation -> {
            Sku saved = invocation.getArgument(0);
            saved.setId(3L);
            saved.setCreatedAt(LocalDateTime.now());
            saved.setUpdatedAt(LocalDateTime.now());
            return saved;
        });

        SkuResponse result = skuService.createSku(newSku);

        assertNotNull(result);
        assertEquals(3L, result.getId());
        assertEquals("SKU003", result.getSkuCode());
        verify(skuRepository).save(any(Sku.class));
    }

    @Test
    @DisplayName("创建SKU - 编码已存在")
    void testCreateSku_CodeExists() {
        when(skuRepository.existsBySkuCode("SKU001")).thenReturn(true);

        Sku newSku = new Sku();
        newSku.setSkuCode("SKU001");
        newSku.setSkuName("测试");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> skuService.createSku(newSku));

        assertEquals("SKU_CODE_EXISTS", exception.getCode());
        verify(skuRepository, never()).save(any());
    }
}
