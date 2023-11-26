.globl
_start:

    li a0, 0x514
    jal label2  # jump to label2 and save position to ra
    li a2, 0x222
    li a3, 0x777

loop:
    j loop

label2:

    li a1, 0x8001
    ret