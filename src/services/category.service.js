import { apiClient, delay, USE_MOCK_API, hasValidLiveAuth, canAccessSellerApi } from './api.js';
import { db } from '../mock/db.js';
import { getCategoryImage, getSubcategoryImage, getBrandLogo } from '../utils/productImages.js';

/**
 * Normalizes a category from backend or seller repository
 */
export function normalizeCategory(cat) {
  if (!cat) return null;
  const categoryId = Number(cat.categoryId ?? cat.id) || cat.categoryId || cat.id;
  const imageUrl = getCategoryImage(cat);
  const subcategories = Array.isArray(cat.subcategories)
    ? cat.subcategories.map((sub, idx) => {
        if (typeof sub === 'string') {
          return {
            id: `sub_${categoryId}_${idx + 1}`,
            subcategoryId: idx + 1,
            name: sub,
            slug: sub.toLowerCase().replace(/\s+/g, '-'),
            imageUrl: getSubcategoryImage({ name: sub, categoryId }, cat),
            brands: [],
          };
        }
        return {
          ...sub,
          id: Number(sub.id || sub.subcategoryId) || sub.id || sub.subcategoryId || `sub_${categoryId}_${idx + 1}`,
          subcategoryId: Number(sub.subcategoryId || sub.id) || sub.subcategoryId || sub.id || idx + 1,
          imageUrl: getSubcategoryImage(sub, cat),
          brands: sub.eligibleBrands || sub.brands || [],
        };
      })
    : [];

  return {
    ...cat,
    id: categoryId,
    categoryId,
    name: cat.name || cat.title || 'Category',
    title: cat.title || cat.name || 'Category',
    slug: cat.slug || String(categoryId),
    imageUrl,
    sortOrder: cat.sortOrder ?? 0,
    active: cat.active ?? true,
    status: cat.status || (cat.active ? 'Active' : 'Pending Approval'),
    productCount: cat.productCount ?? 0,
    subcategories,
    specFields: cat.specFields || [],
  };
}

/**
 * Normalizes a subcategory from backend or seller repository
 */
export function normalizeSubcategory(sub, parentCategory = null) {
  if (!sub) return null;
  const subcategoryId = Number(sub.subcategoryId ?? sub.id) || sub.subcategoryId || sub.id;
  const categoryId = Number(sub.categoryId ?? parentCategory?.id ?? parentCategory?.categoryId) || sub.categoryId || parentCategory?.id;
  const imageUrl = getSubcategoryImage(sub, parentCategory);
  return {
    ...sub,
    id: subcategoryId,
    subcategoryId,
    categoryId,
    categoryName: sub.categoryName ?? parentCategory?.name,
    name: sub.name || sub.title || 'Subcategory',
    title: sub.title || sub.name || 'Subcategory',
    slug: sub.slug || String(subcategoryId),
    imageUrl,
    sortOrder: sub.sortOrder ?? 0,
    active: sub.active ?? true,
    status: sub.status || (sub.active ? 'Active' : 'Pending Approval'),
    productCount: sub.productCount ?? 0,
    brandsCount: Array.isArray(sub.brands) ? sub.brands.length : (sub.brandsCount ?? 0),
    brands: sub.eligibleBrands || sub.brands || [],
  };
}

/**
 * Normalizes a brand from backend or seller repository
 */
export function normalizeBrand(b) {
  if (!b) return null;
  const brandId = Number(b.brandId ?? b.id) || b.brandId || b.id;
  const imageUrl = getBrandLogo(b);
  return {
    ...b,
    id: brandId,
    brandId,
    name: b.name || b.brandName || b.title || 'Brand',
    brandName: b.brandName || b.name || 'Brand',
    slug: b.slug || String(brandId),
    subcategoryId: Number(b.subcategoryId) || b.subcategoryId,
    subcategoryName: b.subcategoryName,
    categoryId: Number(b.categoryId) || b.categoryId,
    categoryName: b.categoryName,
    imageUrl,
    sortOrder: b.sortOrder ?? 0,
    active: b.active ?? true,
    status: b.status || (b.active ? 'Active' : 'Pending Approval'),
    verified: b.verified ?? false,
    productCount: b.productCount ?? 0,
  };
}

/**
 * Helper to fetch all products for the seller across local storage and live backend
 */
async function getSellerProductsForCatalog(cleanId) {
  const sellerId = `seller_${cleanId}`;
  const numSellerId = Number(cleanId);

  const localList = db.getSellerProducts(cleanId) || [];
  const generalList = (db.getProducts() || []).filter((p) => {
    if (!p) return false;
    if (typeof p.id === 'string' && /^prod_\d+$/i.test(p.id)) return false;
    const pSellerId = String(p.sellerId || p.vendorId || '').toLowerCase().trim();
    const pClean = pSellerId.replace(/^seller_/, '');
    const pNum = Number(pClean) || Number(p.vendorId || 0);
    return (
      pSellerId === sellerId.toLowerCase() ||
      pSellerId === cleanId.toLowerCase() ||
      pClean === cleanId.toLowerCase() ||
      (numSellerId > 0 && pNum === numSellerId)
    );
  });

  let backendList = [];
  if (!USE_MOCK_API && canAccessSellerApi('/api/seller/products')) {
    try {
      const response = await apiClient.get('/api/seller/products', { timeout: 6000 });
      const rawData = response?.data?.data || response?.data;
      const prods = Array.isArray(rawData) ? rawData : (rawData?.products || []);
      backendList = prods.filter((p) => {
        if (!p) return false;
        if (typeof p.id === 'string' && /^prod_\d+$/i.test(p.id)) return false;
        const pSellerId = String(p.sellerId || p.vendorId || '').toLowerCase().trim();
        const pClean = pSellerId.replace(/^seller_/, '');
        const pNum = Number(pClean) || Number(p.vendorId || 0);
        return (
          pSellerId === sellerId.toLowerCase() ||
          pSellerId === cleanId.toLowerCase() ||
          pClean === cleanId.toLowerCase() ||
          (numSellerId > 0 && pNum === numSellerId)
        );
      });
    } catch (_) {}
  }

  const allMap = new Map();
  generalList.forEach((p) => allMap.set(String(p.id || p.productId || p.title), p));
  localList.forEach((p) => allMap.set(String(p.id || p.productId || p.title), p));
  backendList.forEach((p) => allMap.set(String(p.id || p.productId || p.title), p));
  return Array.from(allMap.values());
}

export const categoryService = {
  /**
   * 1. GET /api/categories (Seller Isolated)
   * Returns ONLY categories created by or owned by this seller.
   */
  async getCategories(options = { includeSubcategories: true }) {
    const currentSeller = db.getSeller();
    const storedSellerId = typeof localStorage !== 'undefined' ? localStorage.getItem('sellerId') : null;
    const rawSellerId = storedSellerId || currentSeller?.sellerId || currentSeller?.id;
    if (!rawSellerId) return [];
    const cleanId = String(rawSellerId).replace(/^seller_/, '');
    const sellerId = `seller_${cleanId}`;
    const numSellerId = Number(cleanId);

    // 1. Fetch seller-isolated local repository
    const localCats = db.getSellerCategories(cleanId) || [];
    const localSubs = db.getSellerSubcategories(cleanId) || [];

    // 2. Fetch seller's products (live backend + local storage)
    const sellerProducts = await getSellerProductsForCatalog(cleanId);

    // 3. Fetch backend categories (STRICT seller-owned filter)
    let backendCats = [];
    if (!USE_MOCK_API) {
      try {
        const catResponse = await apiClient.get('/api/categories', {
          params: { includeSubcategories: options.includeSubcategories ?? true, page: 1, limit: 250 },
          timeout: 8000,
        });

        const rawCats = Array.isArray(catResponse.data)
          ? catResponse.data
          : (catResponse.data?.data || catResponse.data?.categories || []);

        backendCats = (rawCats || [])
          .filter((c) => {
            if (!c) return false;
            const cSellerId = String(c.sellerId || c.vendorId || '').toLowerCase().trim();
            if (!cSellerId) return false;
            const cClean = cSellerId.replace(/^seller_/, '');
            const cNum = Number(cClean);
            return (
              cSellerId === sellerId.toLowerCase() ||
              cSellerId === cleanId.toLowerCase() ||
              cClean === cleanId.toLowerCase() ||
              (numSellerId > 0 && cNum === numSellerId)
            );
          })
          .map((c) => normalizeCategory(c));
      } catch (apiError) {
        console.warn('API getCategories error, using seller store:', apiError?.message);
      }
    }

    // Build subcategory map for seller
    const subMap = {};
    for (const sub of localSubs) {
      const pId = String(sub.categoryId);
      if (!subMap[pId]) subMap[pId] = [];
      subMap[pId].push(sub);
    }

    const mergedMap = new Map();

    // 1. Backend seller categories
    backendCats.forEach((c) => {
      const catIdStr = String(c.categoryId ?? c.id);
      const attachedSubs = subMap[catIdStr] || c.subcategories || [];
      const norm = normalizeCategory({ ...c, subcategories: attachedSubs, sellerId });
      mergedMap.set(catIdStr, norm);
    });

    // 2. Local seller categories
    localCats.forEach((c) => {
      const catIdStr = String(c.id || c.categoryId);
      const attachedSubs = subMap[catIdStr] || c.subcategories || [];
      const norm = normalizeCategory({ ...c, subcategories: attachedSubs, sellerId });
      mergedMap.set(catIdStr, norm);
    });

    // Auto-populate from seller's products & persist in seller store
    sellerProducts.forEach((p, idx) => {
      const catName = p.categoryName || p.category;
      if (catName && catName !== 'Category') {
        const catId = p.categoryId || `cat_s_${catName.toLowerCase().replace(/[^a-z0-9]+/g, '_')}`;
        const catIdStr = String(catId);
        const existingByName = Array.from(mergedMap.values()).find(
          (c) => c.name.toLowerCase() === catName.toLowerCase()
        );
        if (!existingByName && !mergedMap.has(catIdStr)) {
          const subs = [];
          const subName = p.subcategoryName || p.subcategory;
          if (subName && subName !== 'Subcategory') {
            subs.push({
              id: p.subcategoryId || `sub_s_${idx + 1}`,
              subcategoryId: p.subcategoryId || idx + 1,
              name: subName,
              slug: subName.toLowerCase().replace(/[^a-z0-9]+/g, '-'),
              categoryId: catId,
              brands: p.brandName || p.brand ? [p.brandName || p.brand] : [],
            });
          }
          const norm = normalizeCategory({
            id: catId,
            categoryId: catId,
            name: catName,
            title: catName,
            slug: catName.toLowerCase().replace(/[^a-z0-9]+/g, '-'),
            imageUrl: null,
            active: true,
            status: 'Active',
            productCount: 1,
            subcategories: subs,
            sellerId,
          });
          mergedMap.set(catIdStr, norm);
          db.createSellerCategory(cleanId, norm);
        } else if (existingByName) {
          // Increment product count
          existingByName.productCount = (existingByName.productCount || 0) + 1;
        }
      }
    });

    await delay(30);
    return Array.from(mergedMap.values());
  },

  /**
   * 2. GET /api/subcategories (Seller Isolated Subcategories)
   */
  async getSubcategories(categoryId = null) {
    const currentSeller = db.getSeller();
    const storedSellerId = typeof localStorage !== 'undefined' ? localStorage.getItem('sellerId') : null;
    const rawSellerId = storedSellerId || currentSeller?.sellerId || currentSeller?.id;
    if (!rawSellerId) return [];
    const cleanId = String(rawSellerId).replace(/^seller_/, '');
    const sellerId = `seller_${cleanId}`;
    const numSellerId = Number(cleanId);

    const localSubs = db.getSellerSubcategories(cleanId, categoryId) || [];
    const sellerProducts = await getSellerProductsForCatalog(cleanId);

    let backendSubs = [];
    if (!USE_MOCK_API) {
      try {
        const params = { page: 1, limit: 250, ...(categoryId && categoryId !== 'ALL' ? { categoryId } : {}) };
        const response = await apiClient.get('/api/subcategories', { params, timeout: 8000 });
        const rawList = Array.isArray(response.data)
          ? response.data
          : (response.data?.data || response.data?.subcategories || []);

        backendSubs = (rawList || [])
          .filter((s) => {
            if (!s) return false;
            const sSellerId = String(s.sellerId || s.vendorId || '').toLowerCase().trim();
            if (!sSellerId) return false;
            const sClean = sSellerId.replace(/^seller_/, '');
            const sNum = Number(sClean);
            return (
              sSellerId === sellerId.toLowerCase() ||
              sSellerId === cleanId.toLowerCase() ||
              sClean === cleanId.toLowerCase() ||
              (numSellerId > 0 && sNum === numSellerId)
            );
          })
          .map((s) => normalizeSubcategory(s));
      } catch (apiError) {
        console.warn('API getSubcategories notice:', apiError?.message);
      }
    }

    const mergedMap = new Map();

    backendSubs.forEach((s) => {
      const norm = normalizeSubcategory({ ...s, sellerId });
      mergedMap.set(String(norm.id), norm);
    });

    localSubs.forEach((s) => {
      const subIdStr = String(s.id || s.subcategoryId);
      mergedMap.set(subIdStr, normalizeSubcategory({ ...s, sellerId }));
    });

    // Auto-populate from seller's products & persist in seller store
    sellerProducts.forEach((p, idx) => {
      const subName = p.subcategoryName || p.subcategory;
      if (subName && subName !== 'Subcategory') {
        const subId = p.subcategoryId || `sub_s_${subName.toLowerCase().replace(/[^a-z0-9]+/g, '_')}`;
        const subIdStr = String(subId);
        const existingByName = Array.from(mergedMap.values()).find(
          (s) => s.name.toLowerCase() === subName.toLowerCase()
        );
        if (!existingByName && !mergedMap.has(subIdStr)) {
          const norm = normalizeSubcategory({
            id: subId,
            subcategoryId: subId,
            categoryId: p.categoryId || null,
            categoryName: p.categoryName || p.category || '',
            name: subName,
            title: subName,
            slug: subName.toLowerCase().replace(/[^a-z0-9]+/g, '-'),
            imageUrl: null,
            active: true,
            status: 'Active',
            productCount: 1,
            sellerId,
          });
          mergedMap.set(subIdStr, norm);
          db.createSellerSubcategory(cleanId, norm);
        } else if (existingByName) {
          existingByName.productCount = (existingByName.productCount || 0) + 1;
        }
      }
    });

    await delay(30);
    let result = Array.from(mergedMap.values());
    if (categoryId && categoryId !== 'ALL') {
      result = result.filter((s) => String(s.categoryId) === String(categoryId));
    }
    return result;
  },

  /**
   * 2b. Fetch ALL seller subcategories
   */
  async getAllSubcategories() {
    return this.getSubcategories(null);
  },

  /**
   * 3. GET /api/brands (Seller Isolated Brands)
   */
  async getBrands(options = {}) {
    const { subcategoryId, categoryId, active = true } = typeof options === 'object' && options !== null
      ? options
      : { subcategoryId: options };

    const currentSeller = db.getSeller();
    const storedSellerId = typeof localStorage !== 'undefined' ? localStorage.getItem('sellerId') : null;
    const rawSellerId = storedSellerId || currentSeller?.sellerId || currentSeller?.id;
    if (!rawSellerId) return [];
    const cleanId = String(rawSellerId).replace(/^seller_/, '');
    const sellerId = `seller_${cleanId}`;
    const numSellerId = Number(cleanId);

    const localBrands = db.getSellerBrands(cleanId, { subcategoryId, categoryId }) || [];
    const sellerProducts = await getSellerProductsForCatalog(cleanId);

    let backendBrands = [];
    if (!USE_MOCK_API) {
      try {
        const params = { page: 1, limit: 250 };
        if (subcategoryId && subcategoryId !== 'ALL') params.subcategoryId = subcategoryId;
        if (categoryId && categoryId !== 'ALL') params.categoryId = categoryId;
        if (active !== undefined) params.active = active;

        const response = await apiClient.get('/api/brands', { params, timeout: 8000 });
        const rawList = Array.isArray(response.data)
          ? response.data
          : (response.data?.data || response.data?.brands || []);

        backendBrands = (rawList || [])
          .filter((b) => {
            if (!b) return false;
            const bSellerId = String(b.sellerId || b.vendorId || '').toLowerCase().trim();
            if (!bSellerId) return false;
            const bClean = bSellerId.replace(/^seller_/, '');
            const bNum = Number(bClean);
            return (
              bSellerId === sellerId.toLowerCase() ||
              bSellerId === cleanId.toLowerCase() ||
              bClean === cleanId.toLowerCase() ||
              (numSellerId > 0 && bNum === numSellerId)
            );
          })
          .map((b) => normalizeBrand(b));
      } catch (apiError) {
        console.warn('API getBrands notice:', apiError?.message);
      }
    }

    const mergedMap = new Map();

    backendBrands.forEach((b) => {
      const norm = normalizeBrand({ ...b, sellerId });
      mergedMap.set(String(norm.id), norm);
    });

    localBrands.forEach((b) => {
      const brandIdStr = String(b.id || b.brandId);
      mergedMap.set(brandIdStr, normalizeBrand({ ...b, sellerId }));
    });

    // Auto-populate from seller's products & persist in seller store
    sellerProducts.forEach((p, idx) => {
      const bName = p.brandName || p.brand;
      if (bName && bName !== 'Brand') {
        const brandId = p.brandId || `brand_s_${bName.toLowerCase().replace(/[^a-z0-9]+/g, '_')}`;
        const brandIdStr = String(brandId);
        const existingByName = Array.from(mergedMap.values()).find(
          (b) => b.name.toLowerCase() === bName.toLowerCase()
        );
        if (!existingByName && !mergedMap.has(brandIdStr)) {
          const norm = normalizeBrand({
            id: brandId,
            brandId: brandId,
            name: bName,
            brandName: bName,
            slug: bName.toLowerCase().replace(/[^a-z0-9]+/g, '-'),
            categoryId: p.categoryId || null,
            categoryName: p.categoryName || p.category || '',
            subcategoryId: p.subcategoryId || null,
            subcategoryName: p.subcategoryName || p.subcategory || '',
            imageUrl: null,
            active: true,
            status: 'Active',
            productCount: 1,
            sellerId,
          });
          mergedMap.set(brandIdStr, norm);
          db.createSellerBrand(cleanId, norm);
        } else if (existingByName) {
          existingByName.productCount = (existingByName.productCount || 0) + 1;
        }
      }
    });

    await delay(30);
    let result = Array.from(mergedMap.values());
    if (subcategoryId && subcategoryId !== 'ALL') {
      result = result.filter((b) => String(b.subcategoryId) === String(subcategoryId));
    }
    if (categoryId && categoryId !== 'ALL') {
      result = result.filter((b) => String(b.categoryId) === String(categoryId));
    }
    return result;
  },

  /**
   * 3b. Fetch ALL seller brands
   */
  async getAllBrands() {
    return this.getBrands({ subcategoryId: null, active: true });
  },

  /**
   * 4. Get category by ID / Slug
   */
  async getCategoryById(id) {
    if (!id) return null;
    const cats = await this.getCategories();
    return cats.find(c => String(c.id) === String(id) || String(c.categoryId) === String(id) || c.slug === String(id)) || null;
  },

  /**
   * 5. Get dynamic spec schema
   */
  async getCategorySpecSchema(id) {
    const cat = await this.getCategoryById(id);
    return cat?.specFields || [];
  },

  /**
   * 6. POST /api/brands (Inline Brand Creation / Quick-Add)
   * Dispatches to Backend API with automatic 409 conflict ID resolution and seller store sync
   */
  async createBrand(brandData) {
    const currentSeller = db.getSeller();
    const storedSellerId = typeof localStorage !== 'undefined' ? localStorage.getItem('sellerId') : null;
    const currentAuth = db.getAuth();
    const rawSellerId = storedSellerId || currentAuth?.user?.sellerId || currentAuth?.user?.id || currentSeller?.sellerId || currentSeller?.id || '';
    const cleanId = String(rawSellerId).replace(/^seller_/, '');
    const sellerId = cleanId ? `seller_${cleanId}` : '';
    const name = brandData.name?.trim() || brandData.brandName?.trim() || '';

    let catId = Number(brandData.categoryId);
    let subId = Number(brandData.subcategoryId);

    // Auto-resolve missing categoryId or subcategoryId from seller store
    if (!catId || isNaN(catId)) {
      const sellerCats = db.getSellerCategories(cleanId);
      catId = Number(sellerCats[0]?.id || sellerCats[0]?.categoryId || 61);
    }
    if (!subId || isNaN(subId)) {
      const sellerSubs = db.getSellerSubcategories(cleanId, catId);
      subId = Number(sellerSubs[0]?.id || sellerSubs[0]?.subcategoryId || 110);
    }

    const payload = {
      name,
      brandName: name,
      slug: brandData.slug || name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, ''),
      subcategoryId: subId,
      subcategoryName: brandData.subcategoryName || '',
      categoryId: catId,
      categoryName: brandData.categoryName || '',
      logoUrl: brandData.logoUrl || brandData.imageUrl || null,
      imageUrl: brandData.imageUrl || brandData.logoUrl || null,
      website: brandData.website || '',
      sortOrder: Number(brandData.sortOrder || 1),
      active: brandData.active !== undefined ? brandData.active : true,
      sellerId,
      vendorId: sellerId,
      status: 'Pending Approval',
    };

    let realBrandId = null;
    let createdItem = null;

    if (!USE_MOCK_API) {
      try {
        const response = await apiClient.post('/api/brands', payload);
        createdItem = response.data?.data || response.data;
        realBrandId = Number(createdItem?.id || createdItem?.brandId);
      } catch (apiError) {
        if (apiError.response?.status === 409) {
          try {
            // Conflict resolution: Look up real existing brand ID from backend
            const allBrandsRes = await apiClient.get('/api/brands', {
              params: { search: payload.name, limit: 100 },
              timeout: 4000,
            });
            const allBrands = Array.isArray(allBrandsRes.data)
              ? allBrandsRes.data
              : (allBrandsRes.data?.data || allBrandsRes.data?.brands || []);
            const existing = allBrands.find(
              b => (b.name || b.brandName || '').toLowerCase() === payload.name.toLowerCase() ||
                   (b.slug || '').toLowerCase() === payload.slug.toLowerCase()
            );
            if (existing) {
              realBrandId = Number(existing.id || existing.brandId);
              createdItem = existing;
            }
          } catch (_) {}
        }
      }
    }

    const finalId = realBrandId || Number(createdItem?.id || createdItem?.brandId) || (Math.floor(Date.now() % 1000000) + 3000);
    const localSaved = db.createSellerBrand(cleanId, {
      ...payload,
      id: finalId,
      brandId: finalId,
    });

    return normalizeBrand(localSaved);
  },

  /**
   * 7. POST /api/categories (Create New Catalog Category)
   * Dispatches to Backend API with automatic 409 conflict ID resolution and seller store sync
   */
  async createCategory(categoryData) {
    const currentSeller = db.getSeller();
    const storedSellerId = typeof localStorage !== 'undefined' ? localStorage.getItem('sellerId') : null;
    const currentAuth = db.getAuth();
    const rawSellerId = storedSellerId || currentAuth?.user?.sellerId || currentAuth?.user?.id || currentSeller?.sellerId || currentSeller?.id || '';
    const cleanId = String(rawSellerId).replace(/^seller_/, '');
    const sellerId = cleanId ? `seller_${cleanId}` : '';
    const title = categoryData.name?.trim() || categoryData.title?.trim() || '';

    const payload = {
      name: title,
      title: title,
      slug: categoryData.slug?.trim() || title.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, ''),
      sortOrder: Number(categoryData.sortOrder ?? categoryData.displayOrder ?? 0),
      displayOrder: Number(categoryData.displayOrder ?? categoryData.sortOrder ?? 0),
      description: categoryData.description?.trim() || '',
      imageUrl: categoryData.imageUrl?.trim() || null,
      active: categoryData.active !== undefined ? categoryData.active : true,
      sellerId,
      vendorId: sellerId,
      status: 'Pending Approval',
    };

    let realCatId = null;
    let createdItem = null;

    if (!USE_MOCK_API) {
      try {
        const response = await apiClient.post('/api/categories', payload);
        createdItem = response.data?.data || response.data;
        realCatId = Number(createdItem?.id || createdItem?.categoryId);
      } catch (apiError) {
        if (apiError.response?.status === 409) {
          try {
            // Conflict resolution: Look up real existing category ID from backend
            const allCatsRes = await apiClient.get('/api/categories', { params: { limit: 1000 } });
            const allCats = Array.isArray(allCatsRes.data)
              ? allCatsRes.data
              : (allCatsRes.data?.data || allCatsRes.data?.categories || []);
            const existing = allCats.find(
              c => (c.name || c.title || '').toLowerCase() === payload.name.toLowerCase() ||
                   (c.slug || '').toLowerCase() === payload.slug.toLowerCase()
            );
            if (existing) {
              realCatId = Number(existing.id || existing.categoryId);
              createdItem = existing;
            }
          } catch (_) {}
        }
      }
    }

    const finalId = realCatId || Number(createdItem?.id || createdItem?.categoryId) || (Math.floor(Date.now() % 1000000) + 1000);
    const localSaved = db.createSellerCategory(cleanId, {
      ...payload,
      id: finalId,
      categoryId: finalId,
    });

    return normalizeCategory(localSaved);
  },

  /**
   * 8. POST /api/subcategories (Create New Subcategory)
   * Dispatches to Backend API with automatic 409 conflict ID resolution and seller store sync
   */
  async createSubcategory(subData) {
    const currentSeller = db.getSeller();
    const storedSellerId = typeof localStorage !== 'undefined' ? localStorage.getItem('sellerId') : null;
    const currentAuth = db.getAuth();
    const rawSellerId = storedSellerId || currentAuth?.user?.sellerId || currentAuth?.user?.id || currentSeller?.sellerId || currentSeller?.id || '';
    const cleanId = String(rawSellerId).replace(/^seller_/, '');
    const sellerId = cleanId ? `seller_${cleanId}` : '';
    const name = subData.name?.trim() || subData.title?.trim() || '';

    let catId = Number(subData.categoryId);
    if (!catId || isNaN(catId)) {
      const sellerCats = db.getSellerCategories(cleanId);
      catId = Number(sellerCats[0]?.id || sellerCats[0]?.categoryId || 61);
    }

    const payload = {
      name,
      title: name,
      categoryId: catId,
      categoryName: subData.categoryName || '',
      slug: subData.slug?.trim() || name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, ''),
      sortOrder: Number(subData.sortOrder || 0),
      imageUrl: subData.imageUrl?.trim() || null,
      active: subData.active !== false,
      sellerId,
      vendorId: sellerId,
      status: 'Pending Approval',
    };

    let realSubId = null;
    let createdItem = null;

    if (!USE_MOCK_API) {
      try {
        const response = await apiClient.post('/api/subcategories', payload);
        createdItem = response.data?.data || response.data;
        realSubId = Number(createdItem?.id || createdItem?.subcategoryId);
      } catch (apiError) {
        if (apiError.response?.status === 409) {
          try {
            // Conflict resolution: Look up real existing subcategory ID from backend
            const allSubsRes = await apiClient.get('/api/subcategories', { params: { limit: 1000 } });
            const allSubs = Array.isArray(allSubsRes.data)
              ? allSubsRes.data
              : (allSubsRes.data?.data || allSubsRes.data?.subcategories || []);
            const existing = allSubs.find(
              s => (s.name || s.title || '').toLowerCase() === payload.name.toLowerCase() ||
                   (s.slug || '').toLowerCase() === payload.slug.toLowerCase()
            );
            if (existing) {
              realSubId = Number(existing.id || existing.subcategoryId);
              createdItem = existing;
            }
          } catch (_) {}
        }
      }
    }

    const finalId = realSubId || Number(createdItem?.id || createdItem?.subcategoryId) || (Math.floor(Date.now() % 1000000) + 2000);
    const localSaved = db.createSellerSubcategory(cleanId, {
      ...payload,
      id: finalId,
      subcategoryId: finalId,
    });

    return normalizeSubcategory(localSaved);
  },

  /**
   * 9. PUT /api/categories/:id (Update Category)
   */
  async updateCategory(id, categoryData) {
    const storedSellerId = typeof localStorage !== 'undefined' ? localStorage.getItem('sellerId') : null;
    const currentSeller = db.getSeller();
    const currentAuth = db.getAuth();
    const rawSellerId = storedSellerId || currentAuth?.user?.sellerId || currentAuth?.user?.id || currentSeller?.sellerId || currentSeller?.id || '';
    const sellerId = String(rawSellerId);
    const title = categoryData.name?.trim() || categoryData.title?.trim() || '';

    const payload = {
      name: title,
      title: title,
      slug: categoryData.slug?.trim() || title.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, ''),
      sortOrder: Number(categoryData.sortOrder ?? categoryData.displayOrder ?? 0),
      displayOrder: Number(categoryData.displayOrder ?? categoryData.sortOrder ?? 0),
      description: categoryData.description?.trim() || '',
      imageUrl: categoryData.imageUrl?.trim() || null,
      active: categoryData.active !== undefined ? categoryData.active : true,
    };

    if (!USE_MOCK_API) {
      try {
        await apiClient.put(`/api/categories/${id}`, payload);
      } catch (apiError) {
        console.warn('API updateCategory notice:', apiError?.response?.data || apiError.message);
      }
    }

    const localUpdated = db.updateSellerCategory(sellerId, id, payload);
    return normalizeCategory(localUpdated);
  },

  /**
   * 10. DELETE /api/categories/:id (Delete Category)
   */
  async deleteCategory(id) {
    const storedSellerId = typeof localStorage !== 'undefined' ? localStorage.getItem('sellerId') : null;
    const currentSeller = db.getSeller();
    const currentAuth = db.getAuth();
    const rawSellerId = storedSellerId || currentAuth?.user?.sellerId || currentAuth?.user?.id || currentSeller?.sellerId || currentSeller?.id || '';
    const cleanId = String(rawSellerId).replace(/^seller_/, '');

    if (!USE_MOCK_API) {
      try {
        await apiClient.delete(`/api/categories/${id}`);
      } catch (apiError) {
        console.warn('API deleteCategory notice:', apiError?.response?.data || apiError.message);
      }
    }

    db.deleteSellerCategory(cleanId, id);
    return true;
  },

  /**
   * 11. PUT /api/subcategories/:id (Update Subcategory)
   */
  async updateSubcategory(id, subData) {
    const storedSellerId = typeof localStorage !== 'undefined' ? localStorage.getItem('sellerId') : null;
    const currentSeller = db.getSeller();
    const currentAuth = db.getAuth();
    const rawSellerId = storedSellerId || currentAuth?.user?.sellerId || currentAuth?.user?.id || currentSeller?.sellerId || currentSeller?.id || '';
    const cleanId = String(rawSellerId).replace(/^seller_/, '');
    const name = subData.name?.trim() || subData.title?.trim() || '';

    const payload = {
      name,
      title: name,
      categoryId: Number(subData.categoryId) || null,
      categoryName: subData.categoryName || '',
      slug: subData.slug?.trim() || name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, ''),
      sortOrder: Number(subData.sortOrder || 0),
      imageUrl: subData.imageUrl?.trim() || null,
      active: subData.active !== false,
    };

    if (!USE_MOCK_API) {
      try {
        await apiClient.put(`/api/subcategories/${id}`, payload);
      } catch (apiError) {
        console.warn('API updateSubcategory notice:', apiError?.response?.data || apiError.message);
      }
    }

    const localUpdated = db.updateSellerSubcategory(cleanId, id, payload);
    return normalizeSubcategory(localUpdated);
  },

  /**
   * 12. DELETE /api/subcategories/:id (Delete Subcategory)
   */
  async deleteSubcategory(id) {
    const storedSellerId = typeof localStorage !== 'undefined' ? localStorage.getItem('sellerId') : null;
    const currentSeller = db.getSeller();
    const currentAuth = db.getAuth();
    const rawSellerId = storedSellerId || currentAuth?.user?.sellerId || currentAuth?.user?.id || currentSeller?.sellerId || currentSeller?.id || '';
    const cleanId = String(rawSellerId).replace(/^seller_/, '');

    if (!USE_MOCK_API) {
      try {
        await apiClient.delete(`/api/subcategories/${id}`);
      } catch (apiError) {
        console.warn('API deleteSubcategory notice:', apiError?.response?.data || apiError.message);
      }
    }

    db.deleteSellerSubcategory(cleanId, id);
    return true;
  },

  /**
   * 13. PUT /api/brands/:id (Update Brand)
   */
  async updateBrand(id, brandData) {
    const storedSellerId = typeof localStorage !== 'undefined' ? localStorage.getItem('sellerId') : null;
    const currentSeller = db.getSeller();
    const currentAuth = db.getAuth();
    const rawSellerId = storedSellerId || currentAuth?.user?.sellerId || currentAuth?.user?.id || currentSeller?.sellerId || currentSeller?.id || '';
    const cleanId = String(rawSellerId).replace(/^seller_/, '');
    const name = brandData.name?.trim() || brandData.brandName?.trim() || '';

    const payload = {
      name,
      brandName: name,
      slug: brandData.slug || name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, ''),
      categoryId: Number(brandData.categoryId) || null,
      categoryName: brandData.categoryName || '',
      subcategoryId: Number(brandData.subcategoryId) || null,
      subcategoryName: brandData.subcategoryName || '',
      imageUrl: brandData.imageUrl || null,
      sortOrder: Number(brandData.sortOrder || 1),
      active: brandData.active !== undefined ? brandData.active : true,
    };

    if (!USE_MOCK_API) {
      try {
        await apiClient.put(`/api/brands/${id}`, payload);
      } catch (apiError) {
        console.warn('API updateBrand notice:', apiError?.response?.data || apiError.message);
      }
    }

    const localUpdated = db.updateSellerBrand(cleanId, id, payload);
    return normalizeBrand(localUpdated);
  },

  /**
   * 14. DELETE /api/brands/:id (Delete Brand)
   */
  async deleteBrand(id) {
    const storedSellerId = typeof localStorage !== 'undefined' ? localStorage.getItem('sellerId') : null;
    const currentSeller = db.getSeller();
    const currentAuth = db.getAuth();
    const rawSellerId = storedSellerId || currentAuth?.user?.sellerId || currentAuth?.user?.id || currentSeller?.sellerId || currentSeller?.id || '';
    const cleanId = String(rawSellerId).replace(/^seller_/, '');

    if (!USE_MOCK_API) {
      try {
        await apiClient.delete(`/api/brands/${id}`);
      } catch (apiError) {
        console.warn('API deleteBrand notice:', apiError?.response?.data || apiError.message);
      }
    }

    db.deleteSellerBrand(cleanId, id);
    return true;
  },

  /**
   * 15. POST /api/seller/category-requests (Submit Official Category Request to Admin)
   */
  async submitCategoryRequest(requestData) {
    const catName = requestData.categoryName || requestData.name || '';
    const payload = {
      name: catName,
      categoryName: catName,
      description: requestData.description || '',
    };

    if (!USE_MOCK_API) {
      try {
        const response = await apiClient.post('/api/seller/category-requests', payload);
        return response.data?.data || response.data;
      } catch (err) {
        console.warn('API submitCategoryRequest notice:', err?.response?.data || err.message);
      }
    }

    await delay(200);
    return {
      success: true,
      requestId: `req_${Date.now()}`,
      ...payload,
      status: 'PENDING',
      createdAt: new Date().toISOString(),
    };
  },

  /**
   * 16. GET /api/seller/category-requests (List My Category Requests)
   */
  async getCategoryRequests() {
    if (!USE_MOCK_API) {
      try {
        const response = await apiClient.get('/api/seller/category-requests');
        const list = response.data?.data || response.data;
        if (Array.isArray(list)) return list;
      } catch (err) {
        // Fallback
      }
    }

    await delay(150);
    return [];
  },
};
