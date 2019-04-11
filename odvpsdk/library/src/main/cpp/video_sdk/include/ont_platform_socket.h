#ifndef _ONT_PLATFORM_SOCKET_H_
#define _ONT_PLATFORM_SOCKET_H_


# ifdef __cplusplus
extern "C" {
# endif



/**
 * socket�������ֲʱ���ж������������
 */
typedef struct ont_socket_t ont_socket_t;



/**
 * ����TCP socket����� ���뽫������Ϊ������ģʽ
 * @param  sock ָ�򱣴� TCP socket������ڴ�
 * @return �ɹ��򷵻�ONT_ERR_OK
 */
int ont_platform_tcp_create(ont_socket_t **sock);

/**
 * ����TCP����
 * @param sock socket���
 * @param ip �����IP��ַ
 * @param port ����˶˿�
 * @return �ɹ��򷵻�ONT_ERR_OK������������ӣ�����ONT_ERR_CONNECTING
 */
int ont_platform_tcp_connect(ont_socket_t *sock, const char *ip, uint16_t port);

/**
 * ��������
 * @param sock socket���
 * @param buf  ָ��Ҫ�����͵�����
 * @param size ��Ҫ���������ݵ��ֽ���
 * @param bytes_sent [OUT] �ɹ����͵��ֽ���
 * @return ������������������ONT_ERR_OK
 */
int ont_platform_tcp_send(ont_socket_t *sock, const char *buf,
                          unsigned int size, unsigned int *bytes_sent);
/**
 * ��������
 * @param sock socket���
 * @param buf  ָ��洢�������ݵĻ�����
 * @param size �������ݻ���������ֽ���
 * @param bytes_read [OUT] д�뻺�������ֽ���
 * @return ������������������ONT_ERR_OK
 */
int ont_platform_tcp_recv(ont_socket_t *sock, char *buf,
                          unsigned int size, unsigned int *bytes_read);

/**
 * �ر�TCP socket���
 * @param sock ��Ҫ���رյ�socket���
 */
int ont_platform_tcp_close(ont_socket_t *sock);

//int ont_platform_udp_create(ont_socket_t **sock);

/**
* ����UDP����
* @param sock socket���
* @param ip �����IP��ַ
* @param port ����˶˿�
* @return �ɹ��򷵻�ONT_ERR_OK������������ӣ�����ONT_ERR_CONNECTING
*/
int ont_platform_udp_send(ont_socket_t *sock, char *buf,
                          unsigned int size, unsigned int *bytes_sent);
/**
* ��������
* @param sock socket���
* @param buf  ָ��洢�������ݵĻ�����
* @param size �������ݻ���������ֽ���
* @param bytes_read [OUT] д�뻺�������ֽ���
* @return ������������������ONT_ERR_OK
*/
int ont_platform_udp_recv(ont_socket_t *sock, char *buf,
                          unsigned int size, unsigned int *bytes_read);

/**
* �ر�UDP socket���
* @param sock ��Ҫ���رյ�socket���
*/
int ont_platform_udp_close(ont_socket_t *sock);


/**
* ��ȡsocket fd
*/
int ont_platform_tcp_socketfd(ont_socket_t *sock);

/* 姝ゅ嚱鏁板叧闂璾dp鏂囦欢鎻忚堪绗?
 */
int ont_platform_udp_close_fd(ont_socket_t *sock);

/* 鍒涘缓udp鎻忚堪绗?*/
extern int
ont_platform_udp_create_fd(ont_socket_t *sock);

#ifdef __cplusplus
} /* extern "C" */
#endif




#endif


