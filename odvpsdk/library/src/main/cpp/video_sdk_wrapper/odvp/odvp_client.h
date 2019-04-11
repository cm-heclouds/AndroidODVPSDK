#ifndef ONT_ALL_ODVP_CLIENT_H
#define ONT_ALL_ODVP_CLIENT_H

#include <stdint.h>

# ifdef __cplusplus
extern "C" {
# endif

/**
 * 创建设备，指定产品(product_id),设备的名称(device_name)
 * @param product_id
 * @param name
 * @return
 */
ont_device_t * device_create(ont_device_callback_t *cbs, void * cbs_weak_this);

/**
 *
 * @param product_id
 * @param device_id
 * @return
 */
//int device_connect(ont_device_t * dev, uint64_t device_id);

/**
 *
 * @param product_id
 * @param device_id
 * @param ip
 * @param port
 * @return
 */
int device_connect_by_addr(ont_device_t * dev, uint64_t device_id, char* ip, uint64_t port);

void device_destroy(ont_device_t * dev);

int device_upload_picture(ont_device_t *dev, const char *filename, const char *name, int channel_id);

# ifdef __cplusplus
}
# endif
#endif //ONT_ALL_ODVP_CLIENT_H
