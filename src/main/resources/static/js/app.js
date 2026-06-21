const API_BASE = '/api';

let allSkuList = [];

async function apiGet(url) {
    try {
        const res = await fetch(API_BASE + url);
        const data = await res.json();
        if (data.code === 200) {
            return data.data;
        } else {
            showToast(data.message || '请求失败', 'error');
            return null;
        }
    } catch (e) {
        console.error('API Error:', e);
        showToast('网络错误', 'error');
        return null;
    }
}

async function apiPost(url, body) {
    try {
        const res = await fetch(API_BASE + url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        const data = await res.json();
        if (data.code === 200) {
            return data.data;
        } else {
            showToast(data.message || '操作失败', 'error');
            return null;
        }
    } catch (e) {
        console.error('API Error:', e);
        showToast('网络错误', 'error');
        return null;
    }
}

async function apiPut(url, body) {
    try {
        const res = await fetch(API_BASE + url, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: body ? JSON.stringify(body) : undefined
        });
        const data = await res.json();
        if (data.code === 200) {
            return data.data;
        } else {
            showToast(data.message || '操作失败', 'error');
            return null;
        }
    } catch (e) {
        console.error('API Error:', e);
        showToast('网络错误', 'error');
        return null;
    }
}

function showToast(message, type = 'success') {
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    document.body.appendChild(toast);
    setTimeout(() => {
        toast.remove();
    }, 3000);
}

let _confirmDialogCallback = null;

function showConfirmDialog(title, message, onConfirm, confirmText = '确认', cancelText = '取消') {
    _confirmDialogCallback = onConfirm;

    let dialog = document.getElementById('confirm-dialog');
    if (!dialog) {
        dialog = document.createElement('div');
        dialog.id = 'confirm-dialog';
        dialog.className = 'modal';
        dialog.innerHTML = `
            <div class="modal-content" style="max-width:420px;">
                <div class="modal-header">
                    <h3 id="confirm-dialog-title">确认操作</h3>
                    <span class="close" onclick="closeConfirmDialog()">&times;</span>
                </div>
                <div class="modal-body">
                    <p id="confirm-dialog-message" style="color:#4a5568;font-size:14px;line-height:1.6;"></p>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-secondary" onclick="closeConfirmDialog()" id="confirm-dialog-cancel">取消</button>
                    <button class="btn btn-primary" onclick="executeConfirmDialog()" id="confirm-dialog-confirm">确认</button>
                </div>
            </div>
        `;
        document.body.appendChild(dialog);
    }

    document.getElementById('confirm-dialog-title').textContent = title;
    document.getElementById('confirm-dialog-message').textContent = message;
    document.getElementById('confirm-dialog-confirm').textContent = confirmText;
    document.getElementById('confirm-dialog-cancel').textContent = cancelText;
    dialog.style.display = 'flex';
}

function closeConfirmDialog() {
    const dialog = document.getElementById('confirm-dialog');
    if (dialog) {
        dialog.style.display = 'none';
    }
    _confirmDialogCallback = null;
}

function executeConfirmDialog() {
    const callback = _confirmDialogCallback;
    closeConfirmDialog();
    if (typeof callback === 'function') {
        callback();
    }
}

function updateLastUpdateTime() {
    const el = document.getElementById('last-update');
    if (el) {
        const now = new Date();
        el.textContent = `更新于 ${now.toLocaleTimeString()}`;
    }
}

function getStatusBadge(status) {
    const statusMap = {
        'PENDING': { class: 'badge-warning', text: '待处理' },
        'CONFIRMED': { class: 'badge-info', text: '已确认' },
        'ALLOCATED': { class: 'badge-primary', text: '已分配' },
        'PICKING': { class: 'badge-primary', text: '拣货中' },
        'PICKED': { class: 'badge-success', text: '已拣完' },
        'PACKED': { class: 'badge-info', text: '已打包' },
        'SHIPPED': { class: 'badge-success', text: '已发货' },
        'CANCELLED': { class: 'badge-secondary', text: '已取消' },
        'NEW': { class: 'badge-secondary', text: '新建' },
        'RELEASED': { class: 'badge-info', text: '已释放' },
        'COMPLETED': { class: 'badge-success', text: '已完成' },
        'ASSIGNED': { class: 'badge-info', text: '已分配' }
    };
    const info = statusMap[status] || { class: 'badge-secondary', text: status };
    return `<span class="badge ${info.class}">${info.text}</span>`;
}

function getWaveTypeBadge(type) {
    const typeMap = {
        'NORMAL': { class: 'badge-info', text: '普通' },
        'URGENT': { class: 'badge-danger', text: '紧急' },
        'BULK': { class: 'badge-warning', text: '大宗' }
    };
    const info = typeMap[type] || { class: 'badge-secondary', text: type };
    return `<span class="badge ${info.class}">${info.text}</span>`;
}

async function loadDashboard() {
    const data = await apiGet('/dashboard');
    if (!data) return;

    updateLastUpdateTime();

    const stats = data.stats;
    const html = `
        <div class="stats-grid">
            <div class="stat-card">
                <span class="stat-icon">📋</span>
                <h3>待处理订单</h3>
                <div class="stat-value">${stats.pendingOrderCount}</div>
            </div>
            <div class="stat-card">
                <span class="stat-icon">🌊</span>
                <h3>进行中波次</h3>
                <div class="stat-value">${stats.activeWaveCount}</div>
            </div>
            <div class="stat-card">
                <span class="stat-icon">🛒</span>
                <h3>待拣货任务</h3>
                <div class="stat-value">${stats.pendingTaskCount}</div>
            </div>
            <div class="stat-card">
                <span class="stat-icon">⚠️</span>
                <h3>缺货商品</h3>
                <div class="stat-value">${stats.outOfStockSkuCount}</div>
            </div>
        </div>

        <div class="two-column">
            <div class="section">
                <div class="section-header">
                    <h2>待处理订单</h2>
                    <a href="/orders" style="font-size:13px;color:#4299e1;text-decoration:none;">查看全部 →</a>
                </div>
                <div class="section-body">
                    ${data.pendingOrders.length === 0 ?
                        '<div class="empty-state"><div class="empty-state-icon">📭</div>暂无待处理订单</div>' :
                        `<table class="table">
                            <thead>
                                <tr>
                                    <th>订单号</th>
                                    <th>客户</th>
                                    <th>商品数</th>
                                    <th>状态</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${data.pendingOrders.map(o => `
                                    <tr>
                                        <td>${o.orderNo}${o.status === 'CONFIRMED' && o.urgent ? '<span class="urgent-tag">急</span>' : ''}</td>
                                        <td>${o.customerName}</td>
                                        <td>${o.itemCount}种 / ${o.totalQuantity}件</td>
                                        <td>${getStatusBadge(o.status)}</td>
                                    </tr>
                                `).join('')}
                            </tbody>
                        </table>`
                    }
                </div>
            </div>

            <div class="section">
                <div class="section-header">
                    <h2>进行中波次</h2>
                    <a href="/waves" style="font-size:13px;color:#4299e1;text-decoration:none;">查看全部 →</a>
                </div>
                <div class="section-body">
                    ${data.activeWaves.length === 0 ?
                        '<div class="empty-state"><div class="empty-state-icon">🌊</div>暂无进行中波次</div>' :
                        data.activeWaves.map(w => `
                            <div class="wave-card ${w.waveType === 'URGENT' ? 'urgent' : ''}">
                                <div class="wave-header">
                                    <span class="wave-no">${w.waveNo} ${getWaveTypeBadge(w.waveType)}</span>
                                    ${getStatusBadge(w.status)}
                                </div>
                                <div class="wave-stats">
                                    <span>📦 ${w.orderCount} 个订单</span>
                                    <span>🛒 ${w.completedTaskCount}/${w.taskCount} 任务</span>
                                </div>
                                <div class="progress-bar" style="margin-bottom:8px;">
                                    <div class="progress-fill" style="width: ${w.taskCount > 0 ? (w.completedTaskCount / w.taskCount * 100) : 0}%"></div>
                                </div>
                            </div>
                        `).join('')
                    }
                </div>
            </div>
        </div>

        <div class="section">
            <div class="section-header">
                <h2>库存预警</h2>
                <a href="/inventory" style="font-size:13px;color:#4299e1;text-decoration:none;">查看全部 →</a>
            </div>
            <div class="section-body">
                ${data.lowStockItems.length === 0 ?
                    '<div class="empty-state"><div class="empty-state-icon">✅</div>库存充足，无预警</div>' :
                    `<table class="table">
                        <thead>
                            <tr>
                                <th>SKU编码</th>
                                <th>商品名称</th>
                                <th>库位</th>
                                <th>可用库存</th>
                                <th>安全库存</th>
                                <th>状态</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${data.lowStockItems.map(i => `
                                <tr>
                                    <td>${i.skuCode}</td>
                                    <td>${i.skuName}</td>
                                    <td>${i.location || '-'}</td>
                                    <td class="inventory-low">${i.availableQuantity}</td>
                                    <td>${i.safetyStock}</td>
                                    <td><span class="badge badge-danger">库存不足</span></td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>`
                }
            </div>
        </div>
    `;

    document.getElementById('content').innerHTML = html;
}

async function loadOrders() {
    const orders = await apiGet('/orders');
    if (!orders) return;

    updateLastUpdateTime();

    const html = `
        <div class="section">
            <div class="section-header">
                <h2>订单列表</h2>
                <div style="display:flex;gap:8px;">
                    <select id="order-status-filter" onchange="filterOrders()" style="padding:6px 10px;border:1px solid #cbd5e0;border-radius:6px;">
                        <option value="all">全部状态</option>
                        <option value="PENDING">待确认</option>
                        <option value="CONFIRMED">已确认</option>
                        <option value="ALLOCATED">已分配</option>
                        <option value="PICKING">拣货中</option>
                        <option value="PICKED">已拣完</option>
                        <option value="CANCELLED">已取消</option>
                    </select>
                </div>
            </div>
            <div class="section-body" id="orders-list">
                ${renderOrdersTable(orders)}
            </div>
        </div>
    `;

    document.getElementById('content').innerHTML = html;
}

function renderOrdersTable(orders) {
    if (orders.length === 0) {
        return '<div class="empty-state"><div class="empty-state-icon">📋</div>暂无订单</div>';
    }

    return `
        <table class="table">
            <thead>
                <tr>
                    <th>订单号</th>
                    <th>客户</th>
                    <th>联系方式</th>
                    <th>商品数量</th>
                    <th>波次</th>
                    <th>状态</th>
                    <th>创建时间</th>
                    <th>操作</th>
                </tr>
            </thead>
            <tbody>
                ${orders.map(o => `
                    <tr>
                        <td>${o.orderNo}${o.urgent ? '<span class="urgent-tag">急</span>' : ''}</td>
                        <td>${o.customerName}</td>
                        <td>${o.phone || '-'}</td>
                        <td>${o.items ? o.items.length : 0}种 / ${o.totalQuantity || 0}件</td>
                        <td>${o.waveNo || '-'}</td>
                        <td>${getStatusBadge(o.status)}</td>
                        <td>${formatDate(o.createdAt)}</td>
                        <td>
                            ${o.status === 'PENDING' ? `<button class="btn btn-sm btn-success" onclick="confirmOrder(${o.id})">确认</button>` : ''}
                            ${o.status === 'PENDING' || o.status === 'CONFIRMED' ? `<button class="btn btn-sm btn-danger" onclick="cancelOrder(${o.id})">取消</button>` : ''}
                        </td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;
}

async function filterOrders() {
    const status = document.getElementById('order-status-filter').value;
    let orders;
    if (status === 'all') {
        orders = await apiGet('/orders');
    } else {
        orders = await apiGet(`/orders/status/${status}`);
    }
    if (orders) {
        document.getElementById('orders-list').innerHTML = renderOrdersTable(orders);
    }
}

async function confirmOrder(id) {
    showConfirmDialog(
        '确认订单',
        '确定要确认该订单吗？确认后订单状态将变为已确认。',
        async () => {
            const result = await apiPut(`/orders/${id}/confirm`);
            if (result) {
                showToast('订单确认成功');
                loadOrders();
            }
        },
        '确认订单',
        '取消'
    );
}

async function cancelOrder(id) {
    showConfirmDialog(
        '取消订单',
        '确定要取消该订单吗？取消后订单将无法恢复。',
        async () => {
            const result = await apiPut(`/orders/${id}/cancel`);
            if (result) {
                showToast('订单已取消');
                loadOrders();
            }
        },
        '确认取消',
        '返回'
    );
}

async function showCreateOrderModal() {
    document.getElementById('order-modal').style.display = 'flex';
    document.getElementById('order-customer').value = '';
    document.getElementById('order-address').value = '';
    document.getElementById('order-phone').value = '';
    document.getElementById('order-urgent').checked = false;
    document.getElementById('order-remark').value = '';
    document.getElementById('order-items-container').innerHTML = '';

    await loadSkuList();

    if (allSkuList.length === 0) {
        showToast('商品列表加载失败，请刷新页面重试', 'error');
    }

    addOrderItem();
}

function closeOrderModal() {
    document.getElementById('order-modal').style.display = 'none';
}

async function loadSkuList() {
    if (allSkuList.length > 0) return;
    allSkuList = await apiGet('/skus') || [];
    if (allSkuList.length === 0) {
        console.warn('SKU列表加载为空，可能是接口异常');
    }
}

function addOrderItem() {
    const container = document.getElementById('order-items-container');
    const rowIndex = container.children.length + 1;
    const options = allSkuList.map(s =>
        `<option value="${s.id}">${s.skuCode} - ${s.skuName}</option>`
    ).join('');

    const row = document.createElement('div');
    row.className = 'order-item-row';
    row.dataset.index = rowIndex;
    row.innerHTML = `
        <select style="flex:2;">
            <option value="">请选择商品</option>
            ${options}
        </select>
        <input type="number" placeholder="数量" value="1" min="1" style="flex:1;">
        <button class="btn-remove" onclick="removeOrderItem(this)">×</button>
    `;
    container.appendChild(row);
}

function removeOrderItem(btn) {
    const container = document.getElementById('order-items-container');
    if (container.children.length > 1) {
        btn.parentElement.remove();
        let index = 1;
        container.querySelectorAll('.order-item-row').forEach(row => {
            row.dataset.index = index++;
        });
    } else {
        showToast('至少需要一个商品', 'error');
    }
}

async function createOrder() {
    const customer = document.getElementById('order-customer').value.trim();
    if (!customer) {
        showToast('请输入客户名称', 'error');
        return;
    }

    const items = [];
    const rows = document.querySelectorAll('#order-items-container .order-item-row');
    for (const row of rows) {
        const rowIndex = row.dataset.index;
        const skuId = row.querySelector('select').value;
        const qtyInput = row.querySelector('input').value;
        const qty = parseInt(qtyInput);

        if (!skuId) {
            showToast(`第 ${rowIndex} 行：请选择商品`, 'error');
            return;
        }

        if (!qtyInput || qtyInput.trim() === '') {
            showToast(`第 ${rowIndex} 行：请填写商品数量`, 'error');
            return;
        }

        if (isNaN(qty)) {
            showToast(`第 ${rowIndex} 行：商品数量格式不正确`, 'error');
            return;
        }

        if (qty <= 0) {
            showToast(`第 ${rowIndex} 行：商品数量必须大于 0`, 'error');
            return;
        }

        items.push({ skuId: parseInt(skuId), quantity: qty });
    }

    if (items.length === 0) {
        showToast('请添加至少一个商品', 'error');
        return;
    }

    const request = {
        customerName: customer,
        address: document.getElementById('order-address').value.trim(),
        phone: document.getElementById('order-phone').value.trim(),
        urgent: document.getElementById('order-urgent').checked,
        remark: document.getElementById('order-remark').value.trim(),
        items: items
    };

    const result = await apiPost('/orders', request);
    if (result) {
        showToast('订单创建成功');
        closeOrderModal();
        loadOrders();
    }
}

async function loadInventory() {
    const inventories = await apiGet('/inventories');
    if (!inventories) return;

    updateLastUpdateTime();
    window._allInventories = inventories;

    renderInventoryList(inventories);
}

function renderInventoryList(inventories) {
    const html = `
        <div class="section">
            <div class="section-body" id="inventory-list">
                ${inventories.length === 0 ?
                    '<div class="empty-state"><div class="empty-state-icon">📦</div>暂无库存数据</div>' :
                    `<table class="table">
                        <thead>
                            <tr>
                                <th>SKU编码</th>
                                <th>商品名称</th>
                                <th>分类</th>
                                <th>库位</th>
                                <th>可用库存</th>
                                <th>锁定库存</th>
                                <th>总库存</th>
                                <th>安全库存</th>
                                <th>状态</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${inventories.map(i => `
                                <tr>
                                    <td>${i.skuCode}</td>
                                    <td>${i.skuName}</td>
                                    <td>${i.category || '-'}</td>
                                    <td>${i.location || '-'}</td>
                                    <td class="${i.outOfStock ? 'inventory-low' : 'inventory-normal'}">${i.availableQuantity}</td>
                                    <td>${i.lockedQuantity}</td>
                                    <td>${i.totalQuantity}</td>
                                    <td>${i.safetyStock}</td>
                                    <td>${i.outOfStock ?
                                        '<span class="badge badge-danger">库存不足</span>' :
                                        '<span class="badge badge-success">正常</span>'}
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>`
                }
            </div>
        </div>
    `;

    if (document.getElementById('content')) {
        document.getElementById('content').innerHTML = html;
    } else {
        return html;
    }
}

function filterInventory() {
    const filter = document.getElementById('inventory-filter').value;
    const all = window._allInventories || [];
    let filtered = all;

    if (filter === 'low') {
        filtered = all.filter(i => i.outOfStock);
    } else if (filter === 'normal') {
        filtered = all.filter(i => !i.outOfStock);
    }

    renderInventoryList(filtered);
}

async function loadWaves() {
    const waves = await apiGet('/waves');
    if (!waves) return;

    updateLastUpdateTime();

    const html = `
        <div class="section">
            <div class="section-header">
                <h2>波次列表</h2>
            </div>
            <div class="section-body">
                ${waves.length === 0 ?
                    '<div class="empty-state"><div class="empty-state-icon">🌊</div>暂无波次</div>' :
                    `<table class="table">
                        <thead>
                            <tr>
                                <th>波次号</th>
                                <th>类型</th>
                                <th>订单数</th>
                                <th>SKU数</th>
                                <th>总数量</th>
                                <th>状态</th>
                                <th>创建时间</th>
                                <th>操作</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${waves.map(w => `
                                <tr>
                                    <td>${w.waveNo}</td>
                                    <td>${getWaveTypeBadge(w.waveType)}</td>
                                    <td>${w.totalOrderCount}</td>
                                    <td>${w.totalSkuCount}</td>
                                    <td>${w.totalQuantity}</td>
                                    <td>${getStatusBadge(w.status)}</td>
                                    <td>${formatDate(w.createdAt)}</td>
                                    <td>
                                        ${w.status === 'NEW' ? `<button class="btn btn-sm btn-primary" onclick="releaseWave(${w.id})">释放</button>` : ''}
                                        ${(w.status === 'RELEASED' || w.status === 'PICKING') ?
                                            `<button class="btn btn-sm btn-warning" onclick="rollbackWave(${w.id})">回滚</button>` : ''}
                                        ${w.status === 'PICKING' ?
                                            `<button class="btn btn-sm btn-success" onclick="completeWave(${w.id})">完成</button>` : ''}
                                        <button class="btn btn-sm btn-outline" onclick="viewWaveDetail(${w.id})">详情</button>
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>`
                }
            </div>
        </div>
    `;

    document.getElementById('content').innerHTML = html;
}

function showCreateWaveModal() {
    document.getElementById('wave-type').value = 'NORMAL';
    document.getElementById('wave-max-orders').value = '10';
    document.getElementById('wave-zone').value = '';
    document.getElementById('wave-modal').style.display = 'flex';
}

function closeWaveModal() {
    document.getElementById('wave-modal').style.display = 'none';
}

async function createWave() {
    const request = {
        waveType: document.getElementById('wave-type').value,
        maxOrderCount: parseInt(document.getElementById('wave-max-orders').value),
        zone: document.getElementById('wave-zone').value.trim() || null
    };

    const result = await apiPost('/waves', request);
    if (result) {
        showToast('波次创建成功');
        closeWaveModal();
        loadWaves();
    }
}

async function releaseWave(id) {
    showConfirmDialog(
        '释放波次',
        '确定释放该波次吗？释放后将锁定库存并生成拣货任务。',
        async () => {
            const result = await apiPut(`/waves/${id}/release`);
            if (result) {
                showToast('波次释放成功');
                loadWaves();
            }
        },
        '确认释放',
        '取消'
    );
}

async function rollbackWave(id) {
    showConfirmDialog(
        '回滚波次',
        '确定回滚该波次吗？回滚后将解锁库存并取消拣货任务。',
        async () => {
            const result = await apiPut(`/waves/${id}/rollback`);
            if (result) {
                showToast('波次回滚成功');
                loadWaves();
            }
        },
        '确认回滚',
        '取消'
    );
}

async function completeWave(id) {
    showConfirmDialog(
        '完成波次',
        '确定完成该波次吗？',
        async () => {
            const result = await apiPut(`/waves/${id}/complete`);
            if (result) {
                showToast('波次完成');
                loadWaves();
            }
        },
        '确认完成',
        '取消'
    );
}

function viewWaveDetail(id) {
    showToast('波次详情功能开发中', 'info');
}

async function loadPickingTasks() {
    const tasks = await apiGet('/picking-tasks');
    if (!tasks) return;

    updateLastUpdateTime();
    window._allTasks = tasks;

    renderTaskList(tasks);
}

function renderTaskList(tasks) {
    const html = `
        <div class="section">
            <div class="section-body" id="tasks-list">
                ${tasks.length === 0 ?
                    '<div class="empty-state"><div class="empty-state-icon">🛒</div>暂无拣货任务</div>' :
                    `<table class="table">
                        <thead>
                            <tr>
                                <th>任务号</th>
                                <th>波次</th>
                                <th>商品</th>
                                <th>库位</th>
                                <th>数量</th>
                                <th>已拣</th>
                                <th>拣货员</th>
                                <th>优先级</th>
                                <th>状态</th>
                                <th>操作</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${tasks.map(t => `
                                <tr>
                                    <td>${t.taskNo}</td>
                                    <td>${t.waveNo || '-'}</td>
                                    <td>${t.skuCode} - ${t.skuName}</td>
                                    <td>${t.location || '-'}</td>
                                    <td>${t.quantity}</td>
                                    <td>${t.pickedQuantity || 0}</td>
                                    <td>${t.picker || '-'}</td>
                                    <td>${t.priority}</td>
                                    <td>${getStatusBadge(t.status)}</td>
                                    <td>
                                        ${t.status === 'PENDING' ?
                                            `<button class="btn btn-sm btn-primary" onclick="showAssignModal(${t.id})">分配</button>` : ''}
                                        ${t.status === 'ASSIGNED' ?
                                            `<button class="btn btn-sm btn-info" onclick="startTask(${t.id})">开始</button>` : ''}
                                        ${t.status === 'PICKING' ?
                                            `<button class="btn btn-sm btn-success" onclick="showCompleteModal(${t.id})">完成</button>` : ''}
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>`
                }
            </div>
        </div>
    `;

    if (document.getElementById('content')) {
        document.getElementById('content').innerHTML = html;
    }
}

function filterTasks() {
    const status = document.getElementById('task-status-filter').value;
    const all = window._allTasks || [];
    let filtered = all;

    if (status !== 'all') {
        filtered = all.filter(t => t.status === status);
    }

    renderTaskList(filtered);
}

function showAssignModal(taskId) {
    const modal = document.getElementById('task-modal');
    const body = document.getElementById('task-modal-body');
    body.innerHTML = `
        <div class="form-group">
            <label>拣货员姓名</label>
            <input type="text" id="assign-picker" placeholder="请输入拣货员姓名">
        </div>
        <div style="text-align:right;">
            <button class="btn btn-primary" onclick="assignTask(${taskId})">分配</button>
        </div>
    `;
    modal.style.display = 'flex';
}

function closeTaskModal() {
    document.getElementById('task-modal').style.display = 'none';
}

async function assignTask(taskId) {
    const picker = document.getElementById('assign-picker').value.trim();
    if (!picker) {
        showToast('请输入拣货员姓名', 'error');
        return;
    }

    const result = await apiPut(`/picking-tasks/${taskId}/assign`, { picker });
    if (result) {
        showToast('任务分配成功');
        closeTaskModal();
        loadPickingTasks();
    }
}

async function startTask(taskId) {
    const result = await apiPut(`/picking-tasks/${taskId}/start`);
    if (result) {
        showToast('任务已开始');
        loadPickingTasks();
    }
}

function showCompleteModal(taskId) {
    const task = (window._allTasks || []).find(t => t.id === taskId);
    if (!task) return;

    const modal = document.getElementById('task-modal');
    const body = document.getElementById('task-modal-body');
    body.innerHTML = `
        <div class="form-group">
            <label>任务号：${task.taskNo}</label>
        </div>
        <div class="form-group">
            <label>商品：${task.skuCode} - ${task.skuName}</label>
        </div>
        <div class="form-group">
            <label>应拣数量：${task.quantity}</label>
        </div>
        <div class="form-group">
            <label>实际拣货数量 *</label>
            <input type="number" id="complete-qty" value="${task.quantity}" min="0" max="${task.quantity}">
        </div>
        <div style="text-align:right;">
            <button class="btn btn-success" onclick="completeTask(${taskId})">确认完成</button>
        </div>
    `;
    modal.style.display = 'flex';
}

async function completeTask(taskId) {
    const qty = parseInt(document.getElementById('complete-qty').value);
    if (isNaN(qty) || qty < 0) {
        showToast('请输入有效的拣货数量', 'error');
        return;
    }

    const result = await apiPut(`/picking-tasks/${taskId}/complete`, { pickedQuantity: qty });
    if (result) {
        showToast('任务完成');
        closeTaskModal();
        loadPickingTasks();
    }
}

function formatDate(dateStr) {
    if (!dateStr) return '-';
    try {
        const d = new Date(dateStr);
        return d.toLocaleString('zh-CN', {
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });
    } catch (e) {
        return dateStr;
    }
}
