//defined by user code
bool select();

void main() {
    to_count = ti_count;
    to_instanceCount = ti_instanceCount & int(select());
    to_firstIndex = ti_firstIndex;
    to_baseVertex = ti_baseVertex;
    to_baseInstance = ti_baseInstance;
}
