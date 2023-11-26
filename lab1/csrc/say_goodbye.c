#define UART_SEND_ADDR ((volatile unsigned int *)(0x40000010))


int main() {
    const char* msg = "Never gonna";

    int mem_write_addr = 4;

    for (int i = 0; msg[i] != '\0'; i++) {
        *UART_SEND_ADDR = msg[i];
        *((int*)(mem_write_addr + i * 4)) = msg[i];
    }


    return 0;
}


